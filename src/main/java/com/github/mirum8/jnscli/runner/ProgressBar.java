package com.github.mirum8.jnscli.runner;

import java.util.List;

public interface ProgressBar {
    List<String> running();

    int refreshIntervalMillis();

    List<String> completed();

    List<String> failed();
}
