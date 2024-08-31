package com.github.mirum8.jnscli.jenkins;

public class JenkinsAPIException extends RuntimeException {
    public JenkinsAPIException(String message) {
        super(message);
    }

    public JenkinsAPIException(String message, Throwable cause) {
        super(message, cause);
    }

    public JenkinsAPIException(Throwable cause) {
        super(cause);
    }
}
