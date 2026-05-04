package com.github.mirum8.jnscli.jenkins;

public record NodeDescription(
    String id,
    String name,
    String status,
    String parameterDescription
) {
}
