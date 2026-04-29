package com.github.mirum8.jnscli.ai;

import io.github.ollama4j.Ollama;
import io.github.ollama4j.exceptions.OllamaException;
import io.github.ollama4j.models.generate.OllamaGenerateRequest;
import io.github.ollama4j.utils.OptionsBuilder;

public class OllamaClient implements AiClient {
    private final Ollama ollama;
    private final String model;

    public OllamaClient(LlmSettings.Ollama settings) {
        this.ollama = new Ollama(settings.endpoint());
        ollama.setRequestTimeoutSeconds(60);
        this.model = settings.model();
    }

    @Override
    public String generate(String prompt) {
        try {
            OllamaGenerateRequest request = OllamaGenerateRequest.builder()
                .withModel(model)
                .withPrompt(prompt)
                .withRaw(false)
                .withOptions(new OptionsBuilder().build());
            return ollama.generate(request, null).getResponse();
        } catch (OllamaException e) {
            throw new AiException(e);
        }
    }
}
