package com.github.mirum8.jnscli.shell;

import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;

public class TextFormatter {
    private TextFormatter() {
    }

    public static String colored(String message, TextColor color) {
        return (new AttributedStringBuilder()).append(message, AttributedStyle.DEFAULT.foreground(color.toJLineColorCode())).toAnsi();
    }

    public static String bold(String message) {
        return (new AttributedStringBuilder()).append(message, AttributedStyle.DEFAULT.bold()).toAnsi();
    }
}
