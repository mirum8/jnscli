package com.github.mirum8.jnscli.ai;

import com.github.mirum8.jnscli.runner.CommandParameters;
import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.runner.SpinnerFactory;
import com.github.mirum8.jnscli.shell.Messages;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.exceptions.OllamaException;
import io.github.ollama4j.models.response.Model;

import static com.github.mirum8.jnscli.ai.LlmSettings.Ollama.DEFAULT_ENDPOINT;

public class OllamaSettingsPrompter implements AiSettingsPrompter {

    private final ShellPrompter prompter;
    private final CommandRunner commandRunner;
    private final SpinnerFactory spinnerFactory;
    private final Messages messages;
    private final String model;

    public OllamaSettingsPrompter(ShellPrompter prompter,
                                  CommandRunner commandRunner,
                                  SpinnerFactory spinnerFactory,
                                  Messages messages,
                                  String model) {
        this.prompter = prompter;
        this.commandRunner = commandRunner;
        this.spinnerFactory = spinnerFactory;
        this.messages = messages;
        this.model = model;
    }

    @Override
    public LlmSettings promptSettings() {
        String ollamaEndpoint = prompter.promptString("Enter Ollama endpoint", DEFAULT_ENDPOINT);
        Ollama ollamaApi = new Ollama(ollamaEndpoint);
        try {
            if (!ollamaApi.ping()) {
                throw new IllegalStateException("Ollama endpoint is not reachable");
            }
        } catch (OllamaException e) {
            throw new AiException(e);
        }
        if (!isModelInstalled(ollamaApi)) {
            pullModelWithProgress(ollamaApi);
        }
        return new LlmSettings.Ollama(ollamaEndpoint, model);
    }

    private boolean isModelInstalled(Ollama ollamaApi) {
        try {
            return ollamaApi.listModels().stream()
                .map(Model::getName)
                .anyMatch(name -> name.equals(model) || name.startsWith(model + ":"));
        } catch (OllamaException e) {
            throw new AiException(e);
        }
    }

    private void pullModelWithProgress(Ollama ollamaApi) {
        commandRunner.run(() -> pullModel(ollamaApi), CommandParameters.builder()
            .withProgressBar(spinnerFactory.builder("Pulling " + model).build())
            .onSuccess(ignored -> messages.successText("The model has been successfully downloaded"))
            .onFailure(ignored -> messages.failureText("Failed to download the model"))
            .build());
    }

    private void pullModel(Ollama ollamaApi) {
        try {
            ollamaApi.pullModel(model);
        } catch (OllamaException e) {
            throw new AiException(e);
        }
    }
}
