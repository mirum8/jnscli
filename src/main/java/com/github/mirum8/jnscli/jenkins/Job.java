package com.github.mirum8.jnscli.jenkins;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Job(
    @JsonProperty("_class")
    String aClass,
    String name,
    String url,
    String color
) {
    public Job copyWithName(String name) {
        return new Job(aClass, name, url, color);
    }
}
