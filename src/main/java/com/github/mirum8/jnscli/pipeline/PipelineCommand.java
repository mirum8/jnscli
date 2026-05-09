package com.github.mirum8.jnscli.pipeline;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;

@Component
@Command(group = "Pipeline Commands", description = "Pipeline Commands", command = "pipeline")
public class PipelineCommand {
    private final PipelineCreateService pipelineCreateService;

    public PipelineCommand(PipelineCreateService pipelineCreateService) {
        this.pipelineCreateService = pipelineCreateService;
    }

    @Command(command = "create", description = "Create a pipeline job from a Git repository")
    public void create(String name,
                       @Option(longNames = "repo", shortNames = 'r', description = "Git repository URL") String repo,
                       @Option(longNames = "branch", shortNames = 'b', description = "Branch (default: main)") String branch,
                       @Option(longNames = "script-path", shortNames = 's', description = "Path to Jenkinsfile (default: Jenkinsfile)") String scriptPath,
                       @Option(longNames = "folder", shortNames = 'f', description = "Folder path to create the pipeline in") String folder,
                       @Option(longNames = "credentials", shortNames = 'c', description = "Jenkins credentialsId for the SCM checkout") String credentialsId,
                       @Option(longNames = "description", shortNames = 'd', description = "Job description") String description) {
        pipelineCreateService.create(name, repo, branch, scriptPath, folder, credentialsId, description);
    }
}
