package com.github.mirum8.jnscli.build.parameters;

import com.github.mirum8.jnscli.jenkins.WorkflowJob;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import com.github.mirum8.jnscli.util.FileUtil;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class FileParameterPrompter implements ParameterPrompter {
    private final ShellPrompter shellPrompter;

    public FileParameterPrompter(ShellPrompter shellPrompter) {
        this.shellPrompter = shellPrompter;
    }

    @Override
    public String prompt(WorkflowJob.Property.ParameterDefinition parameterDefinition) {
        String userInput = shellPrompter.promptString(parameterDefinition.name() + "(file path)", null);
        return FileUtil.resolveHomeDir(userInput);
    }

    @Override
    public Set<String> applicableForTypes() {
        return Set.of("FileParameterDefinition");
    }
}
