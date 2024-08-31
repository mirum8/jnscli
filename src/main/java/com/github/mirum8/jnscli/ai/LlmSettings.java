package com.github.mirum8.jnscli.ai;

import com.github.mirum8.jnscli.settings.AiSettings;

import java.util.List;
import java.util.Properties;

public sealed interface LlmSettings extends AiSettings {
    String PROVIDERS_OLLAMA = "ollama";
    String PROPERTIES_AI_PROVIDER = "ai.provider";

    String model();

    final class Ollama implements LlmSettings {

        private static final String PROPERTIES_OLLAMA_ENDPOINT = "ollama.endpoint";
        private static final String PROPERTIES_OLLAMA_MODEL = "ollama.model";
        static final String DEFAULT_ENDPOINT = "http://localhost:11434";

        private final String endpoint;
        private final String model;


        Ollama(String endpoint, String model) {
            this.endpoint = endpoint;
            this.model = model;
        }

        Ollama(Properties props) {
            this(
                props.getProperty(PROPERTIES_OLLAMA_ENDPOINT, DEFAULT_ENDPOINT),
                props.getProperty(PROPERTIES_OLLAMA_MODEL)
            );
        }

        @Override
        public void writeToProps(Properties prop) {
            prop.setProperty(PROPERTIES_OLLAMA_ENDPOINT, endpoint);
            prop.setProperty(PROPERTIES_OLLAMA_MODEL, model);
            prop.setProperty(PROPERTIES_AI_PROVIDER, PROVIDERS_OLLAMA);
        }

        public String endpoint() {
            return endpoint;
        }

        @Override
        public String model() {
            return model;
        }

    }

    static List<String> supportedProviders() {
        return List.of(PROVIDERS_OLLAMA);
    }
}
