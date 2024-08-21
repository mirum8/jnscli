package com.github.mirum8.jnscli.build.parameters;

import com.github.mirum8.jnscli.jenkins.WorkflowJob.Property.ParameterDefinition;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
class StringParameterPrompter implements ParameterPrompter {
    private final ShellPrompter shellPrompter;

    public StringParameterPrompter(ShellPrompter shellPrompter) {
        this.shellPrompter = shellPrompter;
    }

    @Override
    public String prompt(ParameterDefinition parameterDefinition) {
        return shellPrompter.promptString(parameterDefinition.name(), parameterDefinition.defaultValue())
            .replace(" ", "%20");
    }

    @Override
    public Set<String> applicableForTypes() {
        return Set.of("StringParameterDefinition", "TextParameterDefinition", "CredentialsParameterDefinition");
    }
}
