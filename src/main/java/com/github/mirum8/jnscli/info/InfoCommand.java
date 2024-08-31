package com.github.mirum8.jnscli.info;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;

@Component
@Command(group = "Info Commands", description = "Info Commands", command = "info")
public class InfoCommand {
    private final InfoService infoService;

    public InfoCommand(InfoService infoService) {
        this.infoService = infoService;
    }

    @Command(description = "Get information about a job")
    public void info(String jobId,
                     @Option(longNames = "buildNumber", shortNames = 'b') Integer buildNumber,
                     @Option(longNames = "includeSuccess", shortNames = 's') boolean includeSuccess,
                     @Option(longNames = "includeFailed", shortNames = 'f') boolean includeFailed,
                     @Option(longNames = "includeRunning", shortNames = 'r') boolean includeRunning,
                     @Option(longNames = "limit", shortNames = 'l', defaultValue = "3") Integer limit,
                     @Option(longNames = "my-builds", shortNames = 'm') boolean onlyMyBuilds) {
        infoService.info(jobId, buildNumber, includeSuccess, includeFailed, includeRunning, limit, onlyMyBuilds);
    }

    @Command(command = "builds", description = "Get information about job builds")
    public void builds(String jobId,
                       @Option(longNames = "includeSuccess", shortNames = 's') boolean includeSuccess,
                       @Option(longNames = "includeFailed", shortNames = 'f') boolean includeFailed,
                       @Option(longNames = "includeRunning", shortNames = 'r') boolean includeRunning,
                       @Option(longNames = "limit", shortNames = 'l', defaultValue = "3") Integer limit,
                       @Option(longNames = "myBuilds", shortNames = 'm') boolean onlyMyBuilds) {
        infoService.builds(jobId, includeSuccess, includeFailed, includeRunning, limit, onlyMyBuilds);
    }
}
