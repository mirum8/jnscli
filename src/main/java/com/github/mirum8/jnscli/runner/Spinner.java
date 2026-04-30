package com.github.mirum8.jnscli.runner;

import com.github.mirum8.jnscli.shell.Messages;
import com.github.mirum8.jnscli.shell.Symbols;

import java.util.List;

public class Spinner implements ProgressBar {
    private final char[] spinnerChars;
    private final Messages messages;

    private final String runningMessage;
    private final String errorMessage;
    private final String completeMessage;

    private int spinCounter;

    Spinner(Symbols symbols, Messages messages, String runningMessage, String errorMessage, String completeMessage) {
        this.messages = messages;
        this.spinnerChars = symbols.spinnerFrames();
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
        return completeMessage != null
            ? List.of(messages.successText(completeMessage))
            : List.of();
    }

    @Override
    public List<String> failed() {
        return errorMessage != null
            ? List.of(messages.failureText(errorMessage))
            : List.of();
    }

    public static class Builder {
        private final Symbols symbols;
        private final Messages messages;
        private String runningMessage;
        private String errorMessage;
        private String completeMessage;

        Builder(Symbols symbols, Messages messages) {
            this.symbols = symbols;
            this.messages = messages;
        }

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
            return new Spinner(symbols, messages, runningMessage, errorMessage, completeMessage);
        }
    }
}
