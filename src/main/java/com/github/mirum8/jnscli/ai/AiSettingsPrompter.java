package com.github.mirum8.jnscli.ai;

public interface AiSettingsPrompter {
    LlmSettings promptSettings(String llmProvider);
}
