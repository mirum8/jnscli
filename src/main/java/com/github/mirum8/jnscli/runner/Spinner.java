package com.github.mirum8.jnscli.runner;

import com.github.mirum8.jnscli.shell.Symbols;
import com.github.mirum8.jnscli.shell.Theme;

import java.util.List;

public class Spinner implements ProgressBar {
    private final char[] spinnerChars;
    private final Theme theme;
    private final Symbols symbols;

    private final String runningMessage;
    private final String errorMessage;
    private final String completeMessage;

    private int spinCounter;

    Spinner(Theme theme, Symbols symbols, String runningMessage, String errorMessage, String completeMessage) {
        this.theme = theme;
        this.symbols = symbols;
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
            ? List.of(theme.success(symbols.ok()) + " " + completeMessage)
            : List.of();
    }

    @Override
    public List<String> failed() {
        return errorMessage != null
            ? List.of(theme.failure(symbols.fail()) + " " + errorMessage)
            : List.of();
    }

    public static class Builder {
        private final Theme theme;
        private final Symbols symbols;
        private String runningMessage;
        private String errorMessage;
        private String completeMessage;

        Builder(Theme theme, Symbols symbols) {
            this.theme = theme;
            this.symbols = symbols;
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
            return new Spinner(theme, symbols, runningMessage, errorMessage, completeMessage);
        }
    }
}
