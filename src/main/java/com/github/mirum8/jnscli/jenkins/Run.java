package com.github.mirum8.jnscli.jenkins;

import java.util.List;

public record Run(
    Integer id,
    String name,
    Status status,
    List<Stage> stages
) implements Build {

    public record Stage(
        String id,
        String name,
        Status status
    ) {
    }
}


