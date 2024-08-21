package com.github.mirum8.jnscli.util;

public class Strings {
    private Strings() {
    }

    public static boolean isJobNumber(String str) {
        return str.startsWith("%") && Integers.isInteger(str.substring(1));
    }
}
