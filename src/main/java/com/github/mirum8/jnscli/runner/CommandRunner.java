package com.github.mirum8.jnscli.runner;

import com.github.mirum8.jnscli.shell.RefreshableMultilineRenderer;
import com.github.mirum8.jnscli.util.Threads;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class CommandRunner {

    private final RefreshableMultilineRenderer refreshableMultilineRenderer;

    public CommandRunner(RefreshableMultilineRenderer refreshableMultilineRenderer) {
        this.refreshableMultilineRenderer = refreshableMultilineRenderer;
    }

    public <C, R> Result<R> run(Callable<R> operation, CommandParameters<C> commandParameters) {
        try (var progressBarExecutor = Executors.newScheduledThreadPool(0, Thread.ofVirtual().factory())) {
            progressBarExecutor.scheduleAtFixedRate(() -> refreshableMultilineRenderer.render(commandParameters.progressBar().running()),
                0, commandParameters.progressBar().refreshIntervalMillis(), TimeUnit.MILLISECONDS);
            R result = operation.call();
            Result<C> chekingResult = processUntilCompleteOrTimeout(commandParameters);
            progressBarExecutor.shutdown();
            return switch (chekingResult) {
                case Result.Success<?> success -> {
                    processSuccess(commandParameters, (C) success.value());
                    yield new Result.Success<>(result);
                }
                case Result.Failure<?> failure when failure.value() != null -> {
                    processFailure(commandParameters, (C) failure.value());
                    yield new Result.Failure<>(result);
                }
                case Result.Failure<?> ignored -> {
                    precessTimeout(commandParameters);
                    yield new Result.Failure<>(result);
                }
            };
        } catch (Exception e) {
            throw new CommandRunnerException(e);
        } finally {
            refreshableMultilineRenderer.reset();
        }
    }

    public <C> void run(Runnable operation, CommandParameters<C> commandParameters) {
        run(() -> {
            operation.run();
            return null;
        }, commandParameters);
    }

    public <C> Result<Void> showProgress(CommandParameters<C> commandParameters) {
        return run(() -> null, commandParameters);
    }

    private <C> Result<C> processUntilCompleteOrTimeout(CommandParameters<C> commandParameters) {
        Instant timeout = commandParameters.timeout() > 0
            ? Instant.now().plusSeconds(commandParameters.timeout())
            : Instant.MAX;

        while (true) {
            if (timeout.isBefore(Instant.now())) {
                return new Result.Failure<>(null);
            }
            C checkResult = commandParameters.completionChecker().get();
            if (commandParameters.successWhen().test(checkResult)) {
                return new Result.Success<>(checkResult);
            }
            if (commandParameters.failureWhen().test(checkResult)) {
                return new Result.Failure<>(checkResult);
            }
            Threads.sleepSecs(5);
        }
    }

    private <C> void processFailure(CommandParameters<C> commandParameters, C checkResult) {
        refreshableMultilineRenderer.render(commandParameters.progressBar().failed());
        refreshableMultilineRenderer.reset();
        refreshableMultilineRenderer.render(commandParameters.onFailure().apply(checkResult));
    }

    private <C> void processSuccess(CommandParameters<C> commandParameters, C checkResult) {
        refreshableMultilineRenderer.render(commandParameters.progressBar().completed());
        refreshableMultilineRenderer.reset();
        refreshableMultilineRenderer.render(commandParameters.onSuccess().apply(checkResult));
    }

    private void precessTimeout(CommandParameters<?> commandParameters) {
        refreshableMultilineRenderer.render(commandParameters.progressBar().failed());
        refreshableMultilineRenderer.reset();
        refreshableMultilineRenderer.render(commandParameters.timeoutMessage().get());
    }
}
