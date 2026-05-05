package com.github.mirum8.jnscli.ai;

import com.github.mirum8.jnscli.settings.AiSettings;
import com.github.mirum8.jnscli.settings.AiSettingsProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Properties;

import static com.github.mirum8.jnscli.ai.LlmSettings.Ollama.DEFAULT_ENDPOINT;
import static com.github.mirum8.jnscli.ai.LlmSettings.Ollama.PROPERTIES_OLLAMA_ENDPOINT;
import static com.github.mirum8.jnscli.ai.LlmSettings.PROPERTIES_AI_PROVIDER;

@Component
public class LlmSettingsProvider implements AiSettingsProvider {

    private final String ollamaModel;

    public LlmSettingsProvider(@Value("${app.ai.ollama.model}") String ollamaModel) {
        this.ollamaModel = ollamaModel;
    }

    @Override
    public Optional<AiSettings> getFromProps(Properties props) {
        return Optional.ofNullable(props.getProperty(PROPERTIES_AI_PROVIDER))
            .map(provider -> switch (provider) {
                case LlmSettings.PROVIDERS_OLLAMA -> new LlmSettings.Ollama(
                    props.getProperty(PROPERTIES_OLLAMA_ENDPOINT, DEFAULT_ENDPOINT),
                    ollamaModel
                );
                case LlmSettings.PROVIDERS_OPENAI -> new LlmSettings.OpenAI(props);
                default -> null;
            });
    }
}
