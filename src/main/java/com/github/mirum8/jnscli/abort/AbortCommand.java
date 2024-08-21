package com.github.mirum8.jnscli.abort;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent("Abort a job on Jenkins server")
public class AbortCommand {
    private final AbortService abortService;

    public AbortCommand(AbortService abortService) {
        this.abortService = abortService;
    }

    @ShellMethod(key = "abort", value = "Abort a job on Jenkins server")
    public void abort(String jobId) {
        abortService.abort(jobId);
    }
}
