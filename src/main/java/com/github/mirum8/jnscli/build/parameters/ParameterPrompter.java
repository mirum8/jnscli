package com.github.mirum8.jnscli.build.parameters;

import com.github.mirum8.jnscli.jenkins.WorkflowJob.Property.ParameterDefinition;

import java.util.Set;

public interface ParameterPrompter {
    String prompt(ParameterDefinition parameterDefinition);

    Set<String> applicableForTypes();
}
