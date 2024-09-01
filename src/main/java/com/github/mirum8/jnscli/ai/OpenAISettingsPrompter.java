package com.github.mirum8.jnscli.ai;

import com.github.mirum8.jnscli.shell.ShellPrompter;
import io.github.stefanbratanov.jvm.openai.Model;
import io.github.stefanbratanov.jvm.openai.ModelsClient;
import io.github.stefanbratanov.jvm.openai.OpenAI;

import java.util.List;

import static com.github.mirum8.jnscli.ai.LlmSettings.OpenAI.DEFAULT_ENDPOINT;


public class OpenAISettingsPrompter implements AiSettingsPrompter {
    private final ShellPrompter shellPrompter;

    public OpenAISettingsPrompter(ShellPrompter shellPrompter) {
        this.shellPrompter = shellPrompter;
    }

    @Override
    public LlmSettings promptSettings() {
        String openAIEndpoint = shellPrompter.promptString("Enter OpenAI endpoint", DEFAULT_ENDPOINT);
        String apiKey = shellPrompter.promptString("Enter OpenAI API key", null, true);
        ModelsClient modelsClient = OpenAI.newBuilder(apiKey).baseUrl(openAIEndpoint).build().modelsClient();
        List<String> models = modelsClient.listModels().stream().map(Model::id).toList();
        String model = shellPrompter.promptSelectFromList("Please choose a model", models);
        return new LlmSettings.OpenAI(model, apiKey, openAIEndpoint);
    }
}
