package com.github.mirum8.jnscli.creds;

import com.github.mirum8.jnscli.jenkins.Credential;
import com.github.mirum8.jnscli.jenkins.CredentialsAPI;
import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.shell.Messages;
import com.github.mirum8.jnscli.shell.RefreshableMultilineRenderer;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import com.github.mirum8.jnscli.shell.Table;
import com.github.mirum8.jnscli.shell.Theme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class CredsServiceTest {

    private CredentialsAPI credentialsAPI;
    private PasswordGenerator passwordGenerator;
    private CredsFileWriter credsFileWriter;
    private ShellPrompter shellPrompter;
    private Messages messages;
    private CommandRunner commandRunner;
    private CredsService service;

    @BeforeEach
    void setUp() {
        credentialsAPI = mock(CredentialsAPI.class);
        passwordGenerator = mock(PasswordGenerator.class);
        credsFileWriter = mock(CredsFileWriter.class);
        shellPrompter = mock(ShellPrompter.class);
        messages = mock(Messages.class);
        commandRunner = mock(CommandRunner.class);
        Theme theme = mock(Theme.class);
        when(theme.bold(anyString())).thenAnswer(inv -> inv.getArgument(0));
        Table table = mock(Table.class);
        when(table.render(any(), any())).thenReturn(List.of("rendered"));
        RefreshableMultilineRenderer renderer = mock(RefreshableMultilineRenderer.class);
        ShellPrinter printer = mock(ShellPrinter.class);
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(1)).run();
            return null;
        }).when(commandRunner).runWithSpinner(anyString(), any(Runnable.class));

        service = new CredsService(credentialsAPI, passwordGenerator, credsFileWriter,
            shellPrompter, printer, messages, theme, table, renderer, commandRunner);
    }

    @Test
    void addUserPassWithRandomGeneratesPasswordAndWritesFile() {
        when(passwordGenerator.generate()).thenReturn("R4nd0mPa$$w0rd_1234567");
        when(credsFileWriter.write(eq("ci-bot"), eq(CredentialType.USER_PASS), any()))
            .thenReturn(Path.of("/tmp/creds/ci-bot.txt"));

        service.add("ci-bot", "user-pass", "ci", null, null, true, "desc", null);

        verify(credentialsAPI).createUserPass("ci-bot", "ci", "R4nd0mPa$$w0rd_1234567", "desc", "GLOBAL");
        verify(credsFileWriter).write(eq("ci-bot"), eq(CredentialType.USER_PASS), any());
        verify(messages).success("Created credential 'ci-bot'");
        verify(messages).success("Generated credentials saved to:");
    }

    @Test
    void addUserPassWithExplicitPasswordSkipsGeneratorAndFileWriter() {
        service.add("known", "user-pass", "ci", "explicit-value", null, false, null, null);

        verify(credentialsAPI).createUserPass("known", "ci", "explicit-value", null, "GLOBAL");
        verifyNoInteractions(passwordGenerator);
        verifyNoInteractions(credsFileWriter);
        verify(messages).success("Created credential 'known'");
        verify(messages, never()).success("Generated credentials saved to:");
    }

    @Test
    void addUserPassRejectsBothRandomAndExplicitPassword() {
        assertThatThrownBy(() -> service.add("x", "user-pass", "ci", "p", null, true, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("--random");
        verifyNoInteractions(credentialsAPI);
    }

    @Test
    void addSecretTextWithRandomWritesGeneratedSecretToFile() {
        when(passwordGenerator.generate()).thenReturn("Gen3r4t3dS3cr3t!_------");
        when(credsFileWriter.write(eq("api-key"), eq(CredentialType.SECRET_TEXT), any()))
            .thenReturn(Path.of("/tmp/creds/api-key.txt"));

        service.add("api-key", "secret-text", null, null, null, true, null, null);

        verify(credentialsAPI).createSecretText("api-key", "Gen3r4t3dS3cr3t!_------", null, "GLOBAL");
        verify(credsFileWriter).write(eq("api-key"), eq(CredentialType.SECRET_TEXT), any());
    }

    @Test
    void addPromptsForTypeWhenNotSpecified() {
        when(shellPrompter.promptSelectFromList(eq("Type"), any())).thenReturn("secret-text");
        when(passwordGenerator.generate()).thenReturn("AnyValueAtAllRandomized24");

        service.add("api-key", null, null, null, null, true, null, null);

        verify(credentialsAPI).createSecretText(eq("api-key"), anyString(), any(), eq("GLOBAL"));
    }

    @Test
    void removePromptsForConfirmationAndDeletesOnYes() {
        when(shellPrompter.promptForYesNo("Delete credential 'foo'?", false)).thenReturn(true);

        service.remove("foo");

        verify(credentialsAPI).delete("foo");
        verify(messages).success("Deleted credential 'foo'");
    }

    @Test
    void removeSkipsDeleteWhenUserAnswersNo() {
        when(shellPrompter.promptForYesNo(anyString(), eq(false))).thenReturn(false);

        service.remove("foo");

        verifyNoInteractions(credentialsAPI);
        verify(messages).info("Cancelled.");
    }

    @Test
    void listEmptyShowsEmptyMessage() {
        when(credentialsAPI.list()).thenReturn(List.of());

        service.list();

        verify(messages).empty("No credentials configured.");
    }

    @Test
    void listNonEmptyRendersTable() {
        when(credentialsAPI.list()).thenReturn(List.of(
            new Credential("a", "first", "Username with password"),
            new Credential("b", "second", "Secret text")));

        service.list();

        verify(messages, never()).empty(anyString());
    }
}
