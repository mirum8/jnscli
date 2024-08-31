package com.github.mirum8.jnscli.ai;

import com.github.mirum8.jnscli.settings.AiSettings;
import com.github.mirum8.jnscli.settings.AiSettingsProvider;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Properties;

import static com.github.mirum8.jnscli.ai.LlmSettings.PROPERTIES_AI_PROVIDER;

@Component
public class LlmSettingsProvider implements AiSettingsProvider {

    @Override
    public Optional<AiSettings> getFromProps(Properties props) {
        return Optional.ofNullable(props.getProperty(PROPERTIES_AI_PROVIDER))
            .map(provider -> switch (provider) {
                case LlmSettings.PROVIDERS_OLLAMA -> new LlmSettings.Ollama(props);
                default -> null;
            });
    }
}
