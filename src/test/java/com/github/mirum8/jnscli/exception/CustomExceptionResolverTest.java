package com.github.mirum8.jnscli.exception;

import com.github.mirum8.jnscli.jenkins.JenkinsAPIException;
import com.github.mirum8.jnscli.runner.CommandRunnerException;
import com.github.mirum8.jnscli.shell.Messages;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.shell.Symbols;
import com.github.mirum8.jnscli.shell.TerminalCapabilities;
import com.github.mirum8.jnscli.shell.TestCapabilities;
import com.github.mirum8.jnscli.shell.Theme;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.Test;
import org.springframework.shell.command.CommandHandlingResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class CustomExceptionResolverTest {

    @Test
    void commandRunnerExceptionWrappingInterruptedExceptionExitsCleanly() throws IOException {
        CustomExceptionResolver resolver = newResolver();

        CommandHandlingResult result = resolver.resolve(
            new CommandRunnerException(new InterruptedException()));

        assertThat(result.exitCode()).isNull();
        assertThat(result.message()).isEmpty();
    }

    @Test
    void commandRunnerExceptionWrappingJenkinsExceptionWrappingInterruptedExitsCleanly() throws IOException {
        CustomExceptionResolver resolver = newResolver();

        CommandHandlingResult result = resolver.resolve(
            new CommandRunnerException(new JenkinsAPIException(new InterruptedException())));

        assertThat(result.exitCode()).isNull();
        assertThat(result.message()).isEmpty();
    }

    @Test
    void jenkinsExceptionWrappingInterruptedExitsCleanly() throws IOException {
        CustomExceptionResolver resolver = newResolver();

        CommandHandlingResult result = resolver.resolve(
            new JenkinsAPIException(new InterruptedException()));

        assertThat(result.exitCode()).isNull();
        assertThat(result.message()).isEmpty();
    }

    @Test
    void nonInterruptJenkinsExceptionReturnsFailureWithExitCode() throws IOException {
        CustomExceptionResolver resolver = newResolver();

        CommandHandlingResult result = resolver.resolve(
            new JenkinsAPIException("HTTP: 500"));

        assertThat(result.exitCode()).isEqualTo(1);
        assertThat(result.message()).contains("HTTP: 500");
    }

    @Test
    void deeplyNestedNonInterruptCauseChainTerminates() throws IOException {
        CustomExceptionResolver resolver = newResolver();
        RuntimeException deepest = new RuntimeException("boom");
        Throwable current = deepest;
        for (int i = 0; i < 100; i++) {
            current = new RuntimeException("layer " + i, current);
        }

        CommandHandlingResult result = resolver.resolve((RuntimeException) current);

        assertThat(result.exitCode()).isEqualTo(1);
    }

    private static CustomExceptionResolver newResolver() throws IOException {
        TerminalCapabilities caps = TestCapabilities.disabled();
        DumbTerminal terminal = new DumbTerminal("test", "dumb",
            new ByteArrayInputStream(new byte[0]), new ByteArrayOutputStream(), StandardCharsets.UTF_8);
        Messages messages = new Messages(new ShellPrinter(terminal), new Theme(caps), new Symbols(caps));
        return new CustomExceptionResolver(messages);
    }
}
