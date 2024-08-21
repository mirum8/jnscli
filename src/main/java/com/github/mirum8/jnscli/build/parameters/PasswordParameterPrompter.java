package com.github.mirum8.jnscli.build.parameters;

import com.github.mirum8.jnscli.jenkins.WorkflowJob;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class PasswordParameterPrompter implements ParameterPrompter {
    private final ShellPrompter shellPrompter;

    public PasswordParameterPrompter(ShellPrompter shellPrompter) {
        this.shellPrompter = shellPrompter;
    }

    @Override
    public String prompt(WorkflowJob.Property.ParameterDefinition parameterDefinition) {
        return shellPrompter.promptPassword(parameterDefinition.name());
    }

    @Override
    public Set<String> applicableForTypes() {
        return Set.of("PasswordParameterDefinition");
    }
}
