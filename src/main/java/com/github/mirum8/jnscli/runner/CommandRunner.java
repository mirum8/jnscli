package com.github.mirum8.jnscli.runner;

import com.github.mirum8.jnscli.shell.RefreshableMultilineRenderer;
import com.github.mirum8.jnscli.util.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class CommandRunner {

    private static final Logger log = LoggerFactory.getLogger(CommandRunner.class);
    private static final long DRAIN_TIMEOUT_SECONDS = 2;

    private final RefreshableMultilineRenderer refreshableMultilineRenderer;
    private final SpinnerFactory spinnerFactory;

    public CommandRunner(RefreshableMultilineRenderer refreshableMultilineRenderer, SpinnerFactory spinnerFactory) {
        this.refreshableMultilineRenderer = refreshableMultilineRenderer;
        this.spinnerFactory = spinnerFactory;
    }

    public <C, R> Result<R> call(Callable<R> operation, CommandParameters<C> commandParameters) {
        try (var progressBarExecutor = Executors.newScheduledThreadPool(0, Thread.ofVirtual().factory())) {
            progressBarExecutor.scheduleWithFixedDelay(() -> {
                try {
                    refreshableMultilineRenderer.render(commandParameters.progressBar().running());
                } catch (Exception e) {
                    log.warn("Progress bar tick failed; will retry on next interval", e);
                }
            }, 0, commandParameters.progressBar().refreshIntervalMillis(), TimeUnit.MILLISECONDS);
            R result = operation.call();
            Result<C> chekingResult = processUntilCompleteOrTimeout(commandParameters);
            drain(progressBarExecutor);
            return switch (chekingResult) {
                case Result.Success<?>(Object value) -> {
                    processSuccess(commandParameters, (C) value);
                    yield new Result.Success<>(result);
                }
                case Result.Failure<?>(Object value) when value != null -> {
                    processFailure(commandParameters, (C) value);
                    yield new Result.Failure<>(result);
                }
                case Result.Failure<?>(Object ignored) -> {
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

    private static void drain(ScheduledExecutorService executor) {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(DRAIN_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public <C, R> Result<R> callWithSpinner(String spinnerMessage, Callable<R> operation) {
        return call(operation, CommandParameters.<C>builder().withProgressBar(spinnerFactory.builder(spinnerMessage).build()).build());
    }

    public <C> void runWithSpinner(String spinnerMessage, Runnable operation) {
        run(operation, CommandParameters.<C>builder().withProgressBar(spinnerFactory.builder(spinnerMessage).build()).build());
    }

    public <C> void run(Runnable operation, CommandParameters<C> commandParameters) {
        call(() -> {
            operation.run();
            return null;
        }, commandParameters);
    }

    public <C> Result<Void> showProgress(CommandParameters<C> commandParameters) {
        return call(() -> null, commandParameters);
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
