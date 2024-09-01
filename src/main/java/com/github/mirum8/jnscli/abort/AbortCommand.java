package com.github.mirum8.jnscli.abort;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;

@Component
@Command(description = "Abort a job on Jenkins server")
public class AbortCommand {
    private final AbortService abortService;

    public AbortCommand(AbortService abortService) {
        this.abortService = abortService;
    }

    @Command(command = "abort", description = "Abort a job on Jenkins server")
    public void abort(String jobId) {
        abortService.abort(jobId);
    }

    @Command(command = "abort", description = "Abort a specific build of a job on Jenkins server")
    public void abort(String jobId, @Option(shortNames = 'b', longNames = "buildNumber") int buildNumber) {
        abortService.abort(jobId, buildNumber);
    }
}
