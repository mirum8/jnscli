package com.github.mirum8.jnscli.ai;

import com.github.mirum8.jnscli.settings.AiSettings;

import java.util.List;
import java.util.Properties;

public sealed interface LlmSettings extends AiSettings {
    String PROVIDERS_OLLAMA = "ollama";
    String PROVIDERS_OPENAI = "openAI";
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

    final class OpenAI implements LlmSettings {
        private static final String PROPERTIES_OPENAI_MODEL = "openai.model";
        private static final String PROPERTIES_OPENAI_API_KEY = "openai.apiKey";
        private static final String PROPERTIES_OPENAI_ENDPOINT = "openai.endpoint";
        static final String DEFAULT_ENDPOINT = "https://api.openai.com/v1/";

        private final String model;
        private final String apiKey;
        private final String endpoint;

        public OpenAI(String model, String apiKey, String endpoint) {
            this.model = model;
            this.apiKey = apiKey;
            this.endpoint = endpoint;
        }

        public OpenAI(Properties props) {
            this(
                props.getProperty(PROPERTIES_OPENAI_MODEL),
                props.getProperty(PROPERTIES_OPENAI_API_KEY),
                props.getProperty(PROPERTIES_OPENAI_ENDPOINT, DEFAULT_ENDPOINT)
            );
        }

        @Override
        public String model() {
            return model;
        }

        public String apiKey() {
            return apiKey;
        }

        public String endpoint() {
            return endpoint;
        }

        @Override
        public void writeToProps(Properties prop) {
            prop.setProperty(PROPERTIES_OPENAI_MODEL, model);
            prop.setProperty(PROPERTIES_OPENAI_API_KEY, apiKey);
            prop.setProperty(PROPERTIES_OPENAI_ENDPOINT, endpoint);
            prop.setProperty(PROPERTIES_AI_PROVIDER, PROVIDERS_OPENAI);
        }
    }

    static List<String> supportedProviders() {
        return List.of(PROVIDERS_OLLAMA, PROVIDERS_OPENAI);
    }
}
