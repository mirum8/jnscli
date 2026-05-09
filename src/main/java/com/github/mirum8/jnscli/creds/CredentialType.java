package com.github.mirum8.jnscli.creds;

public enum CredentialType {
    USER_PASS("user-pass", "UsernamePassword"),
    SECRET_TEXT("secret-text", "SecretText");

    private final String cliName;
    private final String displayName;

    CredentialType(String cliName, String displayName) {
        this.cliName = cliName;
        this.displayName = displayName;
    }

    public String cliName() {
        return cliName;
    }

    public String displayName() {
        return displayName;
    }

    public static CredentialType fromCliName(String value) {
        for (CredentialType type : values()) {
            if (type.cliName.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown credential type: " + value + " (expected: user-pass or secret-text)");
    }
}
