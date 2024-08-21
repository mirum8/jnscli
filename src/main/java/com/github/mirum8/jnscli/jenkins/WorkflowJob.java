package com.github.mirum8.jnscli.jenkins;

import java.util.List;

public record WorkflowJob(
    String fullName,
    String name,
    String url,
    boolean buildable,
    String color,
    List<HealthReport> healthReport,
    Build lastBuild,
    List<Build> builds,
    List<Property> property,
    int nextBuildNumber,
    String description
) {
    public record Build(
        int number,
        String url
    ) {}

    public record HealthReport (
        int score
    ) {}

    public record Property(
        List<ParameterDefinition> parameterDefinitions
    ) {
        public record ParameterDefinition(
            DefaultParameterValue defaultParameterValue,
            String description,
            String name,
            String type,
            List<String> choices
        ) {
            public record DefaultParameterValue(
                String name,
                String value
            ) {
            }

            public String defaultValue() {
                return defaultParameterValue().value();
            }
        }
    }

    public boolean isRunning() {
        return color().endsWith("_anime");
    }

}
