package com.github.mirum8.jnscli.shell;

import org.springframework.stereotype.Component;

@Component
public class Symbols {
    private static final char[] BRAILLE_FRAMES = {'⠋', '⠙', '⠹', '⠸', '⠼', '⠴', '⠦', '⠧', '⠇', '⠏'};
    private static final char[] ASCII_FRAMES = {'|', '/', '-', '\\'};

    private final TerminalCapabilities caps;

    public Symbols(TerminalCapabilities caps) {
        this.caps = caps;
    }

    public String ok() {
        return caps.supportsUnicode() ? "✓" : "[OK]";
    }

    public String fail() {
        return caps.supportsUnicode() ? "✗" : "[X]";
    }

    public String warn() {
        return caps.supportsUnicode() ? "!" : "[!]";
    }

    public String aborted() {
        return caps.supportsUnicode() ? "⊘" : "[A]";
    }

    public String disabled() {
        return caps.supportsUnicode() ? "⊘" : "[D]";
    }

    public String notbuilt() {
        return caps.supportsUnicode() ? "·" : "[N]";
    }

    public String unknown() {
        return caps.supportsUnicode() ? "?" : "[?]";
    }

    public String running() {
        return caps.supportsUnicode() ? "•" : "*";
    }

    public String activeDot() {
        return caps.supportsUnicode() ? "●" : "*";
    }

    public String pending() {
        return caps.supportsUnicode() ? "○" : "o";
    }

    public String emptyMark() {
        return caps.supportsUnicode() ? "—" : "-";
    }

    public String info() {
        return caps.supportsUnicode() ? "·" : ".";
    }

    public String folder() {
        return caps.supportsUnicode() ? "▸" : "[/]";
    }

    public char[] spinnerFrames() {
        return caps.supportsUnicode() ? BRAILLE_FRAMES : ASCII_FRAMES;
    }
}
