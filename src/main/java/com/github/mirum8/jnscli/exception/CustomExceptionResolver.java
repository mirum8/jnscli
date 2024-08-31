package com.github.mirum8.jnscli.exception;

import com.github.mirum8.jnscli.jenkins.JenkinsAPIException;
import org.jline.reader.UserInterruptException;
import org.springframework.shell.CommandNotFound;
import org.springframework.shell.command.CommandExceptionResolver;
import org.springframework.shell.command.CommandHandlingResult;
import org.springframework.stereotype.Component;

@Component
public class CustomExceptionResolver implements CommandExceptionResolver {

    @Override
    public CommandHandlingResult resolve(Exception ex) {
        return switch (ex) {
            case JenkinsAPIException e when e.getCause() instanceof InterruptedException ->
                CommandHandlingResult.of("Interrupted");
            case UserInterruptException ignored -> CommandHandlingResult.of("User interrupt\n");
            case CommandNotFound ignored -> CommandHandlingResult.of("Command not found. See 'jns help'\n");
            default -> CommandHandlingResult.of("Error: " + ex.getMessage() + "\n", 1);
        };
    }
}
