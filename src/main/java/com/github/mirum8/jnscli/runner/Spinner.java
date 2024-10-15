package com.github.mirum8.jnscli.runner;

import com.github.mirum8.jnscli.shell.TextColor;

import java.util.List;

import static com.github.mirum8.jnscli.shell.TextFormatter.colored;

public class Spinner implements ProgressBar {
    private final char[] spinnerChars = new char[]{'⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'};

    private final String runningMessage;
    private final String errorMessage;
    private final String completeMessage;

    private int spinCounter;

    private Spinner(String runningMessage, String errorMessage, String completeMessage) {
        this.runningMessage = runningMessage;
        this.errorMessage = errorMessage;
        this.completeMessage = completeMessage;
    }

    @Override
    public List<String> running() {
        String message = spinnerChars[spinCounter] + " " + runningMessage;
        spinCounter = (spinCounter + 1) % spinnerChars.length;
        return List.of(message);
    }

    @Override
    public int refreshIntervalMillis() {
        return 100;
    }

    @Override
    public List<String> completed() {
        return completeMessage != null ? List.of(colored("✓ ", TextColor.GREEN) + completeMessage) : List.of();
    }

    @Override
    public List<String> failed() {
        return errorMessage != null ? List.of(colored("✗ ", TextColor.RED) + errorMessage) : List.of();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder builder(String runningMessage) {
        return new Builder().runningMessage(runningMessage);
    }

    public static class Builder {
        private String runningMessage;
        private String errorMessage;
        private String completeMessage;

        public Builder runningMessage(String runningMessage) {
            this.runningMessage = runningMessage;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder completeMessage(String completeMessage) {
            this.completeMessage = completeMessage;
            return this;
        }

        public Spinner build() {
            return new Spinner(runningMessage, errorMessage, completeMessage);
        }
    }
}
