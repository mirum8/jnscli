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
    public void abort(String jobId, @Option(shortNames = 'b', longNames = "buildNumber") Integer buildNumber) {
        if (buildNumber == null) {
            abortService.abort(jobId);
        } else {
            abortService.abort(jobId, buildNumber);
        }
    }
}
