package com.github.mirum8.jnscli.util;

public class FileUtil {
    private FileUtil() {
    }

    public static String resolveHomeDir(String path) {
        if (path.startsWith("~")) {
            return System.getProperty("user.home") + path.substring(1);
        }
        return path;
    }

}
