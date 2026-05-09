package com.github.mirum8.jnscli.creds;

import java.util.regex.Pattern;

public final class CredentialIds {
    private static final Pattern VALID = Pattern.compile("[A-Za-z0-9._-]+");

    private CredentialIds() {
    }

    public static String validate(String id) {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Credential id is required");
        }
        if (!VALID.matcher(id).matches()) {
            throw new IllegalArgumentException("Credential id may contain only letters, digits, '.', '-', '_': " + id);
        }
        return id;
    }
}
