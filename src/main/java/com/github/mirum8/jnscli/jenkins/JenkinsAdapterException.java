package com.github.mirum8.jnscli.jenkins;

public class JenkinsAdapterException extends RuntimeException {
    public JenkinsAdapterException(String message) {
        super(message);
    }

    public JenkinsAdapterException(String message, Throwable cause) {
        super(message, cause);
    }

    public JenkinsAdapterException(Throwable cause) {
        super(cause);
    }
}
