package com.github.mirum8.jnscli.runner;

public sealed interface Result<T> permits Result.Success, Result.Failure {
    T value();

    record Success<V>(V value) implements Result<V> {
    }

    record Failure<V>(V value) implements Result<V> {
    }
}
