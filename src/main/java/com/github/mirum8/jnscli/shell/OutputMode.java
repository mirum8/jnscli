package com.github.mirum8.jnscli.shell;

import java.util.Locale;

public enum OutputMode {
    RICH,
    PLAIN,
    JSON;

    public static OutputMode parse(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.trim().toLowerCase(Locale.ROOT)) {
            case "rich" -> RICH;
            case "plain" -> PLAIN;
            case "json" -> JSON;
            default -> throw new IllegalArgumentException(
                "Unknown output mode: " + value + " (expected: rich, plain, json)");
        };
    }
}
