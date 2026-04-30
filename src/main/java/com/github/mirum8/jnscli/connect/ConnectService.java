package com.github.mirum8.jnscli.connect;

import com.github.mirum8.jnscli.jenkins.CheckConnectionResult;
import com.github.mirum8.jnscli.jenkins.JenkinsAPI;
import com.github.mirum8.jnscli.runner.CommandParameters;
import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.runner.SpinnerFactory;
import com.github.mirum8.jnscli.settings.Settings;
import com.github.mirum8.jnscli.settings.SettingsService;
import com.github.mirum8.jnscli.shell.Messages;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import org.springframework.stereotype.Service;

@Service
class ConnectService {
    private final ShellPrompter shellPrompter;
    private final JenkinsAPI jenkinsAPI;
    private final CommandRunner commandRunner;
    private final SettingsService settingsService;
    private final SpinnerFactory spinnerFactory;
    private final Messages messages;

    ConnectService(ShellPrompter shellPrompter, JenkinsAPI jenkinsAPI, CommandRunner commandRunner, SettingsService settingsService, SpinnerFactory spinnerFactory, Messages messages) {
        this.shellPrompter = shellPrompter;
        this.jenkinsAPI = jenkinsAPI;
        this.commandRunner = commandRunner;
        this.settingsService = settingsService;
        this.spinnerFactory = spinnerFactory;
        this.messages = messages;
    }

    public void connect() {
        String serverName = shellPrompter.promptString("Server Name (start with http:// or https://):", null);
        String username = shellPrompter.promptString("Username:", null);
        String key = shellPrompter.promptString("Token:", null, true).trim();

        Settings settings = new Settings(serverName, username, key);
        settingsService.writeSettings(settings);

        commandRunner.showProgress(CommandParameters.<CheckConnectionResult>builder()
            .withProgressBar(spinnerFactory.builder().runningMessage("Connecting to Jenkins server " + serverName).build())
            .withCompletionChecker(() -> jenkinsAPI.checkConnection(settings))
            .withSuccessWhen(CheckConnectionResult::isSuccess)
            .withFailureWhen(CheckConnectionResult::isFailure)
            .onSuccess(ignored -> messages.successText("Connection established successfully"))
            .onFailure(result -> messages.failureText("Connection failed: " + result.message()))
            .withTimeout(30)
            .build());
    }
}
