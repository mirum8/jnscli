package com.github.mirum8.jnscli.connect;

import com.github.mirum8.jnscli.jenkins.CheckConnectionResult;
import com.github.mirum8.jnscli.jenkins.JenkinsAPI;
import com.github.mirum8.jnscli.runner.CommandParameters;
import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.runner.Spinner;
import com.github.mirum8.jnscli.settings.Settings;
import com.github.mirum8.jnscli.settings.SettingsService;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import org.springframework.stereotype.Service;

import static com.github.mirum8.jnscli.shell.TextColor.GREEN;
import static com.github.mirum8.jnscli.shell.TextColor.RED;
import static com.github.mirum8.jnscli.shell.TextFormatter.colored;

@Service
class ConnectService {
    private final ShellPrompter shellPrompter;
    private final JenkinsAPI jenkinsAPI;
    private final CommandRunner commandRunner;
    private final SettingsService settingsService;

    ConnectService(ShellPrompter shellPrompter, JenkinsAPI jenkinsAPI, CommandRunner commandRunner, SettingsService settingsService) {
        this.shellPrompter = shellPrompter;
        this.jenkinsAPI = jenkinsAPI;
        this.commandRunner = commandRunner;
        this.settingsService = settingsService;
    }

    public void connect() {
        String serverName = shellPrompter.promptString("Server Name (start with http:// or https://):", null);
        String username = shellPrompter.promptString("Username:", null);
        String key = shellPrompter.promptString("Token:", null, true).trim();

        Settings settings = new Settings(serverName, username, key);
        settingsService.writeSettings(settings);

        commandRunner.showProgress(CommandParameters.<CheckConnectionResult>builder()
            .withProgressBar(Spinner.builder().runningMessage("Connecting to Jenkins server " + serverName).build())
            .withCompletionChecker(() -> jenkinsAPI.checkConnection(settings))
            .withSuccessWhen(CheckConnectionResult::isSuccess)
            .withFailureWhen(CheckConnectionResult::isFailure)
            .onSuccess(ignored -> colored("✓", GREEN) + " Connection established successfully")
            .onFailure(result -> colored("✗", RED) + " Connection failed: " + result.message())
            .withTimeout(30)
            .build());
    }
}
