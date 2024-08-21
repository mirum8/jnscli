package com.github.mirum8.jnscli.util;

public class Integers {
    private Integers() {
    }

    public static boolean isInteger(String str) {
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

}
