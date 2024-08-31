package com.github.mirum8.jnscli.ai;

import com.github.mirum8.jnscli.settings.AiSettings;
import org.springframework.stereotype.Component;

@Component
public class AiClientFactory {

    public AiClient create(AiSettings aiSettings) {
        return switch (aiSettings) {
            case LlmSettings.Ollama ollama -> new OllamaClient(ollama);
            default -> throw new IllegalStateException("Unsupported AI settings: " + aiSettings);
        };
    }
}
