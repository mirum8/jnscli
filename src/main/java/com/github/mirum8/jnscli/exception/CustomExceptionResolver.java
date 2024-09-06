package com.github.mirum8.jnscli.exception;

import com.github.mirum8.jnscli.jenkins.JenkinsAPIException;
import org.jline.reader.UserInterruptException;
import org.springframework.shell.CommandNotFound;
import org.springframework.shell.command.CommandExceptionResolver;
import org.springframework.shell.command.CommandHandlingResult;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.logging.Logger;

@Component
public class CustomExceptionResolver implements CommandExceptionResolver {
    private static final Logger log = Logger.getLogger(CustomExceptionResolver.class.getName());

    @Override
    public CommandHandlingResult resolve(Exception ex) {
        return switch (ex) {
            case JenkinsAPIException e when e.getCause() instanceof InterruptedException ->
                CommandHandlingResult.of("Interrupted");
            case UserInterruptException ignored -> CommandHandlingResult.of("User interrupt\n");
            case CommandNotFound ignored -> CommandHandlingResult.of("Command not found. See 'jns help'\n");
            default -> {
                log.severe(() -> "Error: " + Arrays.toString(ex.getStackTrace()));
                yield CommandHandlingResult.of("Error: " + ex.getMessage(), 1);
            }
        };
    }
}
