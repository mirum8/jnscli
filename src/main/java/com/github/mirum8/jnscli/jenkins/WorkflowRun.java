package com.github.mirum8.jnscli.jenkins;

import java.util.List;

public record WorkflowRun(
    Integer id,
    String name,
    Status status,
    long startTimeMillis,
    long endTimeMillis,
    long durationMillis,
    long queueDurationMillis,
    long pauseDurationMillis,
    List<Stage> stages
) implements Build {
    public record Stage(
        String id,
        String name,
        String status,
        long startTimeMillis,
        long durationMillis,
        long pauseDurationMillis,
        List<StageFlowNode> stageFlowNodes
    ) {

    }

    public record StageFlowNode(
        String id,
        String name,
        String status,
        long startTimeMillis,
        long durationMillis,
        long pauseDurationMillis,
        List<String> parentNodes
    ) {

    }
}
