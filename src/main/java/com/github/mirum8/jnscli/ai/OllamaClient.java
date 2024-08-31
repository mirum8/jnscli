package com.github.mirum8.jnscli.ai;

import io.github.ollama4j.OllamaAPI;
import io.github.ollama4j.exceptions.OllamaBaseException;
import io.github.ollama4j.utils.OptionsBuilder;

import java.io.IOException;

public class OllamaClient implements AiClient {
    private final OllamaAPI ollamaAPI;
    private final String model;

    public OllamaClient(LlmSettings.Ollama settings) {
        this.ollamaAPI = new OllamaAPI(settings.endpoint());
        ollamaAPI.setRequestTimeoutSeconds(60);
        this.model = settings.model();
    }

    @Override
    public String generate(String prompt) {
        try {
            return ollamaAPI.generate(model, prompt, false, new OptionsBuilder().build()).getResponse();
        } catch (OllamaBaseException | IOException e) {
            throw new AiException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AiException(e);
        }
    }
}
