package com.github.mirum8.jnscli.build;

import com.github.mirum8.jnscli.shell.Symbols;
import com.github.mirum8.jnscli.shell.TerminalCapabilities;
import com.github.mirum8.jnscli.shell.Theme;
import org.springframework.stereotype.Component;

@Component
public class PercentageBar {

    private static final int MAX_PROGRESS = 100;
    private static final int MIN_BAR_SIZE = 8;
    private static final int MAX_BAR_SIZE = 24;
    private static final int RESERVED_FOR_LABELS = 30;
    private static final String UNICODE_DONE = "█";
    private static final String UNICODE_REMAINS = "░";
    private static final String[] UNICODE_PARTIAL = {"", "▏", "▎", "▍", "▌", "▋", "▊", "▉"};
    private static final String ASCII_DONE = "=";
    private static final String ASCII_REMAINS = " ";

    private final TerminalCapabilities caps;
    private final Theme theme;
    private final Symbols symbols;

    public PercentageBar(TerminalCapabilities caps, Theme theme, Symbols symbols) {
        this.caps = caps;
        this.theme = theme;
        this.symbols = symbols;
    }

    public String of(int percentage, String statusMessage) {
        return generateBar(percentage, statusMessage, false, symbols.activeDot());
    }

    public String running(int percentage, String statusMessage, String runningIcon) {
        return generateBar(percentage, statusMessage, false, runningIcon);
    }

    public String error(int percentage, String statusMessage) {
        return generateBar(percentage, statusMessage, true, symbols.activeDot());
    }

    public String doneIcon() {
        return caps.supportsUnicode() ? theme.success(symbols.ok()) : "";
    }

    private String generateBar(int percentage, String label, boolean isError, String runningIcon) {
        int clamped = Math.clamp(percentage, 0, MAX_PROGRESS);
        int barSize = barSize();
        State state = state(clamped, isError);
        String text = label == null ? "" : label;

        if (caps.supportsUnicode()) {
            String bar = unicodeBar(clamped, barSize, state);
            return String.format("%s %s %3d%%  %s", themedIcon(state, runningIcon), bar, clamped, text);
        }
        String bar = asciiBar(clamped, barSize, state);
        return String.format("[%s] %3d%%  %s", bar, clamped, text);
    }

    private String unicodeBar(int percentage, int size, State state) {
        int eighths = percentage * size * 8 / MAX_PROGRESS;
        int full = eighths / 8;
        int partial = eighths % 8;
        StringBuilder filled = new StringBuilder();
        filled.append(UNICODE_DONE.repeat(full));
        if (partial > 0 && full < size) {
            filled.append(UNICODE_PARTIAL[partial]);
            full++;
        }
        int remainsSize = Math.max(0, size - full);
        String done = colorByState(filled.toString(), state);
        String remains = theme.dim(UNICODE_REMAINS.repeat(remainsSize));
        return done + remains;
    }

    private String asciiBar(int percentage, int size, State state) {
        int doneSize = percentage * size / MAX_PROGRESS;
        int remainsSize = Math.max(0, size - doneSize);
        String done = colorByState(ASCII_DONE.repeat(doneSize), state);
        return done + ASCII_REMAINS.repeat(remainsSize);
    }

    private State state(int percentage, boolean isError) {
        if (isError) {
            return State.FAILED;
        }
        return percentage >= MAX_PROGRESS ? State.DONE : State.RUNNING;
    }

    private String colorByState(String s, State state) {
        return switch (state) {
            case RUNNING -> theme.warning(s);
            case DONE -> theme.success(s);
            case FAILED -> theme.failure(s);
        };
    }

    private String themedIcon(State state, String runningIcon) {
        return switch (state) {
            case RUNNING -> theme.warning(runningIcon);
            case DONE -> theme.success(symbols.ok());
            case FAILED -> theme.failure(symbols.fail());
        };
    }

    private int barSize() {
        int budget = caps.width() - RESERVED_FOR_LABELS;
        return Math.clamp(budget, MIN_BAR_SIZE, MAX_BAR_SIZE);
    }

    private enum State {
        RUNNING, DONE, FAILED
    }
}
