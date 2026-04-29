package com.github.mirum8.jnscli.build;

import com.github.mirum8.jnscli.shell.TerminalCapabilities;
import com.github.mirum8.jnscli.shell.Theme;
import org.springframework.stereotype.Component;

@Component
public class PercentageBar {

    private static final int MAX_PROGRESS = 100;
    private static final int MIN_BAR_SIZE = 10;
    private static final int MAX_BAR_SIZE = 40;
    private static final int RESERVED_FOR_LABELS = 30;
    private static final String UNICODE_DONE = "█";
    private static final String UNICODE_REMAINS = "░";
    private static final String ASCII_DONE = "=";
    private static final String ASCII_REMAINS = " ";

    private final TerminalCapabilities caps;
    private final Theme theme;

    public PercentageBar(TerminalCapabilities caps, Theme theme) {
        this.caps = caps;
        this.theme = theme;
    }

    public String of(int percentage, String statusMessage) {
        return generateBar(percentage, statusMessage, false);
    }

    public String error(int percentage, String statusMessage) {
        return generateBar(percentage, statusMessage, true);
    }

    private String generateBar(int percentage, String statusMessage, boolean isError) {
        int clamped = Math.clamp(percentage, 0, MAX_PROGRESS);
        int barSize = barSize();
        int doneSize = clamped * barSize / MAX_PROGRESS;
        int remainsSize = barSize - doneSize;

        String doneMarker = caps.supportsUnicode() ? UNICODE_DONE : ASCII_DONE;
        String remainsMarker = caps.supportsUnicode() ? UNICODE_REMAINS : ASCII_REMAINS;

        String done = colorDone(repeat(doneMarker, doneSize), clamped, isError);
        String remains = caps.supportsUnicode() ? theme.dim(repeat(remainsMarker, remainsSize)) : repeat(remainsMarker, remainsSize);

        return String.format("[%s%s] %3d%% %s", done, remains, clamped, statusMessage);
    }

    private String colorDone(String done, int percentage, boolean isError) {
        if (isError) {
            return theme.failure(done);
        }
        if (percentage < MAX_PROGRESS) {
            return theme.warning(done);
        }
        return theme.success(done);
    }

    private int barSize() {
        int budget = caps.width() - RESERVED_FOR_LABELS;
        return Math.clamp(budget, MIN_BAR_SIZE, MAX_BAR_SIZE);
    }

    private static String repeat(String s, int n) {
        return n > 0 ? s.repeat(n) : "";
    }
}
