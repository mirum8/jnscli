package com.github.mirum8.jnscli.runner;

import com.github.mirum8.jnscli.shell.RefreshableMultilineRenderer;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.util.Threads;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

@Component
public class CommandRunner {
    private static final Logger log = Logger.getLogger(CommandRunner.class.getName());

    private final ShellPrinter shellPrinter;
    private final RefreshableMultilineRenderer refreshableMultilineRenderer;
    private volatile boolean running = true;
    private final AtomicLong intervalMultiplier = new AtomicLong(1);

    public CommandRunner(ShellPrinter shellPrinter, RefreshableMultilineRenderer refreshableMultilineRenderer) {
        this.shellPrinter = shellPrinter;
        this.refreshableMultilineRenderer = refreshableMultilineRenderer;
    }

    public <C> RunningResult start(Runnable operation, OperationParameters<C> operationParameters) {
        running = true;
        try {
            startProgressBarInVirtualThread(operationParameters);
            operation.run();
            return processUntilCompleteOrTimeout(operationParameters);
        } finally {
            running = false;
            refreshableMultilineRenderer.reset();
        }
    }

    public <C> RunningResult showProgress(OperationParameters<C> operationParameters) {
        return start(() -> {
        }, operationParameters);
    }

    private <C> RunningResult processUntilCompleteOrTimeout(OperationParameters<C> operationParameters) {
        Instant timeout = operationParameters.timeout() > 0
            ? Instant.now().plusSeconds(operationParameters.timeout())
            : Instant.MAX;

        int attempts = 0;
        while (running) {
            if (timeout.isBefore(Instant.now())) {
                running = false;
                precessTimeout(operationParameters);
                return RunningResult.FAILURE;
            }
            C checkResult = null;
            try {
                checkResult = operationParameters.completionChecker().get();
            } catch (Exception e) {
                intervalMultiplier.updateAndGet(x -> x * 2);
                if (attempts++ > 5) {
                    shellPrinter.println("Failed to check operation completion. Check build status on the job page.");
                    running = false;
                }
            }
            if (operationParameters.successWhen().test(checkResult)) {
                running = false;
                completeProgressBar(operationParameters);
                processSuccess(operationParameters, checkResult);
                return RunningResult.SUCCESS;
            }
            if (operationParameters.failureWhen().test(checkResult)) {
                running = false;
                processFailure(operationParameters, checkResult);
                return RunningResult.FAILURE;
            }
            Threads.sleepSecs(5 * intervalMultiplier.get());
        }
        return RunningResult.FAILURE;
    }

    private <C> void processFailure(OperationParameters<C> operationParameters, C checkResult) {
        pauseForProgressRenderingRefresh(operationParameters);
        if (operationParameters.progressBar().notHideAfterCompletion()) {
            refreshableMultilineRenderer.reset();
        }
        refreshableMultilineRenderer.render(operationParameters.onFailure().apply(checkResult));
    }

    private <C> void processSuccess(OperationParameters<C> operationParameters, C checkResult) {
        pauseForProgressRenderingRefresh(operationParameters);
        if (operationParameters.progressBar().notHideAfterCompletion()) {
            refreshableMultilineRenderer.reset();
        }
        refreshableMultilineRenderer.render(operationParameters.onSuccess().apply(checkResult));
    }

    private <C> void precessTimeout(OperationParameters<C> operationParameters) {
        pauseForProgressRenderingRefresh(operationParameters);
        refreshableMultilineRenderer.reset();
        refreshableMultilineRenderer.render(operationParameters.timeoutMessage().get());
    }

    private void pauseForProgressRenderingRefresh(OperationParameters<?> operationParameters) {
        Threads.sleepMillis(operationParameters.progressBar().refreshIntervalMillis());
    }

    private void startProgressBarInVirtualThread(OperationParameters<?> operationParameters) {
        Thread.ofVirtual().start(() -> {
            int attempts = 0;
            while (running) {
                try {
                    Threads.sleepMillis((operationParameters.progressBar().refreshIntervalMillis() * intervalMultiplier.get()));
                    refreshableMultilineRenderer.render(operationParameters.progressBar().runningMessage());
                } catch (Exception e) {
                    intervalMultiplier.updateAndGet(x -> x * 2);
                    log.severe("Failed to render progress bar: " + e.getMessage() + "; " + "Increasing refresh interval: " + intervalMultiplier + ". Attempts: " + attempts);
                    if (attempts++ > 5) {
                        shellPrinter.println("Progress bar rendering failed. Check build status on the job page.");
                        running = false;
                    }
                }
            }
        });
    }

    private void completeProgressBar(OperationParameters<?> operationParameters) {
        refreshableMultilineRenderer.render(operationParameters.progressBar().runningMessage());
    }

}
