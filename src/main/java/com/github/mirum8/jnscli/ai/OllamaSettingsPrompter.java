package com.github.mirum8.jnscli.ai;

import com.github.mirum8.jnscli.runner.CommandParameters;
import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.runner.SpinnerFactory;
import com.github.mirum8.jnscli.shell.Messages;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import io.github.ollama4j.Ollama;
import io.github.ollama4j.exceptions.OllamaException;
import io.github.ollama4j.models.response.Model;

import java.util.List;
import java.util.Set;

import static com.github.mirum8.jnscli.ai.LlmSettings.Ollama.DEFAULT_ENDPOINT;

public class OllamaSettingsPrompter implements AiSettingsPrompter {
    private static final String PHI = "phi3.5";
    private static final String LLAMA = "llama3.1";

    private final ShellPrompter prompter;
    private final CommandRunner commandRunner;
    private final SpinnerFactory spinnerFactory;
    private final Messages messages;

    private final Set<String> recommendedModels = Set.of(PHI, LLAMA);

    public OllamaSettingsPrompter(ShellPrompter prompter, CommandRunner commandRunner, SpinnerFactory spinnerFactory, Messages messages) {
        this.prompter = prompter;
        this.commandRunner = commandRunner;
        this.spinnerFactory = spinnerFactory;
        this.messages = messages;
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
        List<String> models = listModels(ollamaApi);
        String model;
        if (models.isEmpty() || models.stream().map(m -> m.split(":")[0]).noneMatch(recommendedModels::contains)) {
            model = suggestPullingRecommendedModels(ollamaApi);
        } else {
            model = prompter.promptSelectFromList("Please choose a model", models);
        }
        return new LlmSettings.Ollama(ollamaEndpoint, model);
    }

    private String suggestPullingRecommendedModels(Ollama ollamaAPI) {
        String selected = prompter.promptSelectFromList("Please choose a model for downloading", List.of(LLAMA + ":latest", PHI + ":latest"));
        commandRunner.run(() -> pullModel(ollamaAPI, selected), CommandParameters.builder()
            .withProgressBar(spinnerFactory.builder("Pulling " + selected).build())
            .onSuccess(ignored -> messages.successText("The model has been successfully downloaded"))
            .onFailure(ignored -> messages.failureText("Failed to download the model"))
            .build());
        return selected;
    }

    private void pullModel(Ollama ollamaAPI, String model) {
        try {
            ollamaAPI.pullModel(model);
        } catch (OllamaException e) {
            throw new AiException(e);
        }
    }

    private List<String> listModels(Ollama ollamaApi) {
        try {
            return ollamaApi.listModels().stream().map(Model::getName).toList();
        } catch (OllamaException e) {
            throw new AiException(e);
        }
    }
}
