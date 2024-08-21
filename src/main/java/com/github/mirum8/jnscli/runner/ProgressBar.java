package com.github.mirum8.jnscli.runner;

import java.util.List;

public interface ProgressBar {
    List<String> runningMessage();

    int refreshIntervalMillis();

    default boolean notHideAfterCompletion() {
        return true;
    }
}
