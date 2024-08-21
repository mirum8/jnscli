package com.github.mirum8.jnscli.util;

public class Threads {
    private Threads() {
    }

    public static void sleepSecs(long secs) {
        try {
            Thread.sleep(secs * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void sleepMillis(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
