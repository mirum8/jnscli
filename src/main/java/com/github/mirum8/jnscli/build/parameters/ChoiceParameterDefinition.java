package com.github.mirum8.jnscli.build.parameters;

import com.github.mirum8.jnscli.jenkins.WorkflowJob;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
class ChoiceParameterDefinition implements ParameterPrompter {
    private final ShellPrompter shellPrompter;

    public ChoiceParameterDefinition(ShellPrompter shellPrompter) {
        this.shellPrompter = shellPrompter;
    }

    @Override
    public String prompt(WorkflowJob.Property.ParameterDefinition parameterDefinition) {
        return shellPrompter.promptSelectFromList(parameterDefinition.name(), parameterDefinition.choices());
    }

    @Override
    public Set<String> applicableForTypes() {
        return Set.of("ChoiceParameterDefinition");
    }
}
