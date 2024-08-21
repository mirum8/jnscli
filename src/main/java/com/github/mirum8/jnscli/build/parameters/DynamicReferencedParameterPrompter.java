package com.github.mirum8.jnscli.build.parameters;

import com.github.mirum8.jnscli.jenkins.WorkflowJob;

import java.util.Map;
import java.util.Set;

public interface DynamicReferencedParameterPrompter {
    String prompt(WorkflowJob job, WorkflowJob.Property.ParameterDefinition parameterDefinition, Map<String, String> referenceParameters);
    Set<String> applicableForTypes();
}
