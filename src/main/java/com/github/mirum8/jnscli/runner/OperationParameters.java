package com.github.mirum8.jnscli.runner;

import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public record OperationParameters<C>(
    ProgressBar progressBar,
    Supplier<C> completionChecker,
    Predicate<C> successWhen,
    Predicate<C> failureWhen,
    Function<C, String> onSuccess,
    Function<C, String> onFailure,
    int timeout,
    Supplier<String> timeoutMessage
) {
    public static <C> OperationParametersBuilder<C> builder() {
        return new OperationParametersBuilder<>();
    }

    public static class OperationParametersBuilder<C> {
        private ProgressBar progressBar;
        private Supplier<C> completionChecker = () -> null;
        private Predicate<C> successWhen = value -> true;
        private Predicate<C> failureWhen = value -> false;
        private Function<C, String> onSuccess;
        private Function<C, String> onFailure;
        private int timeout = -1;
        private Supplier<String> timeoutMessage = () -> "Operation timed out";

        public OperationParametersBuilder<C> withProgressBar(ProgressBar progressBar) {
            this.progressBar = progressBar;
            return this;
        }

        public OperationParametersBuilder<C> withCompletionChecker(Supplier<C> completionChecker) {
            this.completionChecker = completionChecker;
            return this;
        }

        public OperationParametersBuilder<C> withSuccessWhen(Predicate<C> successWhen) {
            this.successWhen = successWhen;
            return this;
        }

        public OperationParametersBuilder<C> withFailureWhen(Predicate<C> failureWhen) {
            this.failureWhen = failureWhen;
            return this;
        }

        public OperationParametersBuilder<C> onSuccess(Function<C, String> onSuccess) {
            this.onSuccess = onSuccess;
            return this;
        }

        public OperationParametersBuilder<C> onFailure(Function<C, String> onFailure) {
            this.onFailure = onFailure;
            return this;
        }

        public OperationParametersBuilder<C> withTimeout(int seconds) {
            this.timeout = seconds;
            return this;
        }

        public OperationParametersBuilder<C> onTimeoutError(Supplier<String> timeoutMessage) {
            this.timeoutMessage = timeoutMessage;
            return this;
        }

        public OperationParameters<C> build() {
            return new OperationParameters<>(
                progressBar,
                completionChecker,
                successWhen,
                failureWhen,
                onSuccess,
                onFailure,
                timeout,
                timeoutMessage
            );
        }
    }
}
