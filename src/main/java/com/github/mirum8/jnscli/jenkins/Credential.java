package com.github.mirum8.jnscli.jenkins;

public record Credential(
    String id,
    String description,
    String typeName
) {
}
