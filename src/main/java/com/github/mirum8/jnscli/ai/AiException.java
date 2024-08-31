package com.github.mirum8.jnscli.ai;

public class AiException extends RuntimeException {
    public AiException() {
        super();
    }

    public AiException(String message) {
        super(message);
    }

    public AiException(String message, Throwable cause) {
        super(message, cause);
    }

    public AiException(Throwable cause) {
        super(cause);
    }
}
