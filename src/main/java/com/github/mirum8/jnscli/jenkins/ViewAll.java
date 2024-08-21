package com.github.mirum8.jnscli.jenkins;

import java.util.List;

public record ViewAll(
    String _class,
    List<AssignedLabel> assignedLabels,
    String mode,
    String nodeDescription,
    String nodeName,
    int numExecutors,
    String description,
    List<Job> jobs,
    PrimaryView primaryView,
    String quietDownReason,
    boolean quietingDown,
    int slaveAgentPort,
    UnlabeledLoad unlabeledLoad,
    String url,
    boolean useCrumbs,
    boolean useSecurity,
    List<View> views
) {
    public record AssignedLabel(String name) {
    }

    public record PrimaryView(String _class, String name, String url) {
    }

    public record UnlabeledLoad(String _class) {
    }

    public record View(String _class, String name, String url) {
    }
}
