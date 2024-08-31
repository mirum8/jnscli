package com.github.mirum8.jnscli.ai;

import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.runner.OperationParameters;
import com.github.mirum8.jnscli.runner.Spinner;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import com.github.mirum8.jnscli.shell.TextColor;
import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.models.response.Model;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Set;

import static com.github.mirum8.jnscli.ai.LlmSettings.Ollama.DEFAULT_ENDPOINT;
import static com.github.mirum8.jnscli.shell.TextFormatter.colored;

public class OllamaSettingsPrompter implements AiSettingsPrompter {
    private static final String PHI = "phi3.5";
    private static final String LLAMA = "llama3.1";

    private final ShellPrompter prompter;
    private final CommandRunner commandRunner;

    private final Set<String> recommendedModels = Set.of(PHI, LLAMA);

    public OllamaSettingsPrompter(ShellPrompter prompter, CommandRunner commandRunner) {
        this.prompter = prompter;
        this.commandRunner = commandRunner;
    }

    @Override
    public LlmSettings promptSettings(String llmProvider) {
        String ollamaEndpoint = prompter.promptString("Enter Ollama endpoint", DEFAULT_ENDPOINT);
        OllamaAPI ollamaApi = new OllamaAPI(ollamaEndpoint);
        if (!ollamaApi.ping()) {
            throw new IllegalStateException("Ollama endpoint is not reachable");
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

    private String suggestPullingRecommendedModels(OllamaAPI ollamaAPI) {
        String selected = prompter.promptSelectFromList("Please choose a model for downloading", List.of(LLAMA + ":latest", PHI + ":latest"));
        commandRunner.start(() -> pullModel(ollamaAPI, selected), OperationParameters.builder()
            .withProgressBar(new Spinner("Pulling " + selected))
            .onSuccess(ignored -> colored("✓ ", TextColor.GREEN) + "The model has been successfully downloaded")
            .onFailure(ignored -> colored("✗ ", TextColor.RED) + "Failed to download the model")
            .build());
        return selected;
    }

    private void pullModel(OllamaAPI ollamaAPI, String model) {
        try {
            ollamaAPI.pullModel(model);
        } catch (OllamaBaseException | IOException | URISyntaxException e) {
            throw new AiException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException(e);
        }
    }

    private List<String> listModels(OllamaAPI ollamaApi) {
        try {
            return ollamaApi.listModels().stream().map(Model::getName).toList();
        } catch (OllamaBaseException | IOException | URISyntaxException e) {
            throw new AiException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException(e);
        }
    }
}
