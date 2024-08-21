package com.github.mirum8.jnscli.jenkins;

public record CheckConnectionResult(
    Status status,
    String message
) {
    public enum Status {
        SUCCESS,
        FAILURE
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isFailure() {
        return status == Status.FAILURE;
    }
}
