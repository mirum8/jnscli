package com.github.mirum8.jnscli.util;

import com.github.mirum8.jnscli.jenkins.Status;
import com.github.mirum8.jnscli.shell.Theme;
import org.springframework.stereotype.Component;

@Component
public class StatusFormatter {
    private final Theme theme;

    public StatusFormatter(Theme theme) {
        this.theme = theme;
    }

    public String colored(Status status) {
        return switch (status) {
            case SUCCESS -> theme.success(status.toString());
            case FAILED, FAILURE -> theme.failure(status.toString());
            case ABORTED, IN_PROGRESS -> theme.warning(status.toString());
            default -> status.name();
        };
    }
}
