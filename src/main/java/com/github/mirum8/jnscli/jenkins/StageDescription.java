package com.github.mirum8.jnscli.jenkins;

import java.util.List;

public record StageDescription(
    String id,
    String status,
    List<FlowNode> stageFlowNodes
) {

    public record FlowNode(
        String id,
        String status
    ) {
    }

}
