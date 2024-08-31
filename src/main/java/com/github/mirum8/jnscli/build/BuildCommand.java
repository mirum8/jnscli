package com.github.mirum8.jnscli.build;

import org.springframework.shell.command.CommandRegistration;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Command(group = "Build Commands", description = "Build Commands")
public class BuildCommand {
    private final BuildService buildService;

    public BuildCommand(BuildService buildService) {
        this.buildService = buildService;
    }

    @Command(command = "build", description = "Build a job on Jenkins server")
    public void build(String jobId,
                      @Option(longNames = "quiet", shortNames = 'q') boolean quiet,
                      @Option(longNames = "log", shortNames = 'l') boolean showLog,
                      @Option(arity = CommandRegistration.OptionArity.ZERO_OR_MORE, longNames = "params", shortNames = 'p') List<String> parameters,
                      @Option(longNames = "ai") boolean useAi) {
        buildService.build(jobId, !quiet, !quiet && showLog, parameters, useAi);

    }
}
