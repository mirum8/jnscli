package com.github.mirum8.jnscli.jenkins;

public record ProgressiveConsoleText(String text, boolean hasMoreData, long nextStart) {
}
