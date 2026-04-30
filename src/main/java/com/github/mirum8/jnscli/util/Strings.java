package com.github.mirum8.jnscli.util;

public class Strings {
    private Strings() {
    }

    public static boolean isJobNumber(String str) {
        return str.startsWith("%") && Integers.isInteger(str.substring(1));
    }

    public static String[] splitOnFirst(String input, char delimiter) {
        int idx = input.indexOf(delimiter);
        if (idx < 0) {
            return new String[]{input};
        }
        return new String[]{input.substring(0, idx), input.substring(idx + 1)};
    }
}
