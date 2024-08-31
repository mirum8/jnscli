package com.github.mirum8.jnscli.util;

import com.github.mirum8.jnscli.jenkins.Status;
import com.github.mirum8.jnscli.shell.TextColor;

import static com.github.mirum8.jnscli.shell.TextFormatter.colored;

public class Statuses {
    private Statuses() {
    }

    public static String getColored(Status status) {
        return switch (status) {
            case SUCCESS -> colored(status.toString(), TextColor.GREEN);
            case FAILED, FAILURE -> colored(status.toString(), TextColor.RED);
            case ABORTED, IN_PROGRESS -> colored(status.toString(), TextColor.YELLOW);
            default -> status.name();
        };
    }
}
