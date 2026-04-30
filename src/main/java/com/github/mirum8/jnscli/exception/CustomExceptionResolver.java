package com.github.mirum8.jnscli.exception;

import com.github.mirum8.jnscli.shell.Messages;
import org.jline.reader.UserInterruptException;
import org.springframework.core.NestedExceptionUtils;
import org.springframework.shell.CommandNotFound;
import org.springframework.shell.command.CommandExceptionResolver;
import org.springframework.shell.command.CommandHandlingResult;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.logging.Logger;

@Component
public class CustomExceptionResolver implements CommandExceptionResolver {
    private static final Logger log = Logger.getLogger(CustomExceptionResolver.class.getName());

    private final Messages messages;

    public CustomExceptionResolver(Messages messages) {
        this.messages = messages;
    }

    @Override
    public CommandHandlingResult resolve(Exception ex) {
        if (NestedExceptionUtils.getMostSpecificCause(ex) instanceof InterruptedException) {
            return CommandHandlingResult.of("");
        }
        return switch (ex) {
            case UserInterruptException ignored -> CommandHandlingResult.of("User interrupt\n");
            case CommandNotFound ignored -> CommandHandlingResult.of("Command not found. See 'jns help'\n");
            default -> {
                log.severe(() -> "Error: " + Arrays.toString(ex.getStackTrace()));
                String message = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
                yield CommandHandlingResult.of(messages.failureText(message) + "\n", 1);
            }
        };
    }
}
