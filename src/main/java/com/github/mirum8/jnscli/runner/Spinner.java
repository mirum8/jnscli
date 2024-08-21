package com.github.mirum8.jnscli.runner;

import java.util.List;

public class Spinner implements ProgressBar {
    private final char[] spinnerChars = new char[]{'⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'};

    private final String message;
    private int spinCounter;

    public Spinner(String message) {
        this.message = message;
    }

    @Override
    public List<String> runningMessage() {
        String runningMessage = spinnerChars[spinCounter] + " " + message;
        spinCounter = (spinCounter + 1) % spinnerChars.length;
        return List.of(runningMessage);
    }

    @Override
    public int refreshIntervalMillis() {
        return 100;
    }

    @Override
    public boolean notHideAfterCompletion() {
        return false;
    }
}
