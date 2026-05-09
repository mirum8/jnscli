package com.github.mirum8.jnscli.jenkins;

public record Crumb(
    String crumb,
    String crumbRequestField
) {
}
