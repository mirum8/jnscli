package com.github.mirum8.jnscli.shell;

import org.jline.utils.AttributedStyle;

public enum TextColor {
    RED(AttributedStyle.RED),
    GREEN(AttributedStyle.GREEN),
    YELLOW(AttributedStyle.YELLOW),
    CYAN(AttributedStyle.CYAN);

    private final int code;

    TextColor(int code) {
        this.code = code;
    }

    public int toJLineColorCode() {
        return this.code;
    }
}
