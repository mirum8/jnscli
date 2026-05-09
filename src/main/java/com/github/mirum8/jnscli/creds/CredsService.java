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
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CredsService {
    private static final String DEFAULT_SCOPE = "GLOBAL";

    private final CredentialsAPI credentialsAPI;
    private final PasswordGenerator passwordGenerator;
    private final CredsFileWriter credsFileWriter;
    private final ShellPrompter shellPrompter;
    private final ShellPrinter shellPrinter;
    private final Messages messages;
    private final Theme theme;
    private final Table table;
    private final RefreshableMultilineRenderer renderer;
    private final CommandRunner commandRunner;

    public CredsService(CredentialsAPI credentialsAPI,
                        PasswordGenerator passwordGenerator,
                        CredsFileWriter credsFileWriter,
                        ShellPrompter shellPrompter,
                        ShellPrinter shellPrinter,
                        Messages messages,
                        Theme theme,
                        Table table,
                        RefreshableMultilineRenderer renderer,
                        CommandRunner commandRunner) {
        this.credentialsAPI = credentialsAPI;
        this.passwordGenerator = passwordGenerator;
        this.credsFileWriter = credsFileWriter;
        this.shellPrompter = shellPrompter;
        this.shellPrinter = shellPrinter;
        this.messages = messages;
        this.theme = theme;
        this.table = table;
        this.renderer = renderer;
        this.commandRunner = commandRunner;
    }

    public void list() {
        List<Credential> credentials = credentialsAPI.list();
        if (credentials.isEmpty()) {
            messages.empty("No credentials configured.");
            return;
        }
        List<Table.Column> columns = List.of(
            new Table.Column(theme.bold("ID"), Table.Align.LEFT, new Table.Auto()),
            new Table.Column(theme.bold("Type"), Table.Align.LEFT, new Table.Auto()),
            Table.Column.truncate(theme.bold("Description"), 40)
        );
        List<List<String>> rows = credentials.stream()
            .map(c -> List.of(
                nullToEmpty(c.id()),
                nullToEmpty(c.typeName()),
                nullToEmpty(c.description())))
            .toList();
        renderer.render(table.render(columns, rows));
    }

    public void add(String id, String typeName, String username, String password, String secret,
                    boolean random, String description, String scope) {
        CredentialIds.validate(id);
        CredentialType type = resolveType(typeName);
        String effectiveScope = scope == null || scope.isBlank() ? DEFAULT_SCOPE : scope;

        switch (type) {
            case USER_PASS -> addUserPass(id, username, password, random, description, effectiveScope);
            case SECRET_TEXT -> addSecretText(id, secret, random, description, effectiveScope);
        }
    }

    public void remove(String id) {
        CredentialIds.validate(id);
        if (!shellPrompter.promptForYesNo("Delete credential '" + id + "'?", false)) {
            messages.info("Cancelled.");
            return;
        }
        commandRunner.runWithSpinner("Deleting credential...", () -> credentialsAPI.delete(id));
        messages.success("Deleted credential '" + id + "'");
    }

    private void addUserPass(String id, String username, String password, boolean random, String description, String scope) {
        if (random && password != null) {
            throw new IllegalArgumentException("--random and --password cannot be used together");
        }
        String effectiveUsername = username == null || username.isBlank()
            ? shellPrompter.promptString("Username", null)
            : username;
        if (effectiveUsername == null || effectiveUsername.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }
        String effectivePassword;
        Path generatedFile = null;
        if (random) {
            effectivePassword = passwordGenerator.generate();
        } else if (password != null) {
            effectivePassword = password;
        } else {
            effectivePassword = shellPrompter.promptPassword("Password");
        }
        commandRunner.runWithSpinner("Saving credential...",
            () -> credentialsAPI.createUserPass(id, effectiveUsername, effectivePassword, description, scope));
        if (random) {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("username", effectiveUsername);
            fields.put("password", effectivePassword);
            generatedFile = credsFileWriter.write(id, CredentialType.USER_PASS, fields);
        }
        printCreated(id, generatedFile);
    }

    private void addSecretText(String id, String secret, boolean random, String description, String scope) {
        if (random && secret != null) {
            throw new IllegalArgumentException("--random and --secret cannot be used together");
        }
        String effectiveSecret;
        Path generatedFile = null;
        if (random) {
            effectiveSecret = passwordGenerator.generate();
        } else if (secret != null) {
            effectiveSecret = secret;
        } else {
            effectiveSecret = shellPrompter.promptPassword("Secret");
        }
        commandRunner.runWithSpinner("Saving credential...",
            () -> credentialsAPI.createSecretText(id, effectiveSecret, description, scope));
        if (random) {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("secret", effectiveSecret);
            generatedFile = credsFileWriter.write(id, CredentialType.SECRET_TEXT, fields);
        }
        printCreated(id, generatedFile);
    }

    private CredentialType resolveType(String typeName) {
        if (typeName == null || typeName.isBlank()) {
            String selection = shellPrompter.promptSelectFromList("Type",
                List.of(CredentialType.USER_PASS.cliName(), CredentialType.SECRET_TEXT.cliName()));
            return CredentialType.fromCliName(selection);
        }
        return CredentialType.fromCliName(typeName);
    }

    private void printCreated(String id, Path generatedFile) {
        messages.success("Created credential '" + id + "'");
        if (generatedFile != null) {
            messages.success("Generated credentials saved to:");
            shellPrinter.println("  " + generatedFile);
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
