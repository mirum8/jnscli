package com.github.mirum8.jnscli.diagnose;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;

@Component
@Command
public class ErrorCommand {
    private final ErrorService errorService;

    public ErrorCommand(ErrorService errorService
    ) {
        this.errorService = errorService;
    }

    @Command(command = "error", description = "Get error for the last or specific build")
    void error(
            String jobId,
            @Option(longNames = "buildNumber", shortNames = 'b') Integer buildNumber,
            @Option(longNames = "myBuild", shortNames = 'm') boolean myBuild,
            @Option(longNames = "ai") boolean useAi
    ) {
        errorService.getError(jobId, buildNumber, myBuild, useAi);
    }
}
