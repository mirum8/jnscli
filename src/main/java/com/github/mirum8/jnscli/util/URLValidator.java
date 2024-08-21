package com.github.mirum8.jnscli.util;

import java.net.MalformedURLException;
import java.net.URI;

public class URLValidator {
    private URLValidator() {
    }

    public static void check(String urlStr) {
        try {
            URI.create(urlStr).toURL();
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid URL: " + urlStr, e);
        }
    }

}
