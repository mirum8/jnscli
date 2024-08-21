package com.github.mirum8.jnscli.build.parameters;

import com.github.mirum8.jnscli.jenkins.WorkflowJob;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
class BooleanParameterPrompter implements ParameterPrompter {
    private final ShellPrompter shellPrompter;

    BooleanParameterPrompter(ShellPrompter shellPrompter) {
        this.shellPrompter = shellPrompter;
    }

    @Override
    public String prompt(WorkflowJob.Property.ParameterDefinition parameterDefinition) {
        return String.valueOf(shellPrompter.promptForYesNo(parameterDefinition.name(), Boolean.parseBoolean(parameterDefinition.defaultValue())));
    }

    @Override
    public Set<String> applicableForTypes() {
        return Set.of("BooleanParameterDefinition");
    }
}
