package com.github.mirum8.jnscli.ai;

import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import org.springframework.stereotype.Component;

@Component
public class AiSettingsPrompterFactory {
    private final ShellPrompter shellPrompter;
    private final CommandRunner commandRunner;

    public AiSettingsPrompterFactory(ShellPrompter shellPrompter, CommandRunner commandRunner) {
        this.shellPrompter = shellPrompter;
        this.commandRunner = commandRunner;
    }

    public AiSettingsPrompter create(String aiProvider) {
        return switch (aiProvider) {
            case LlmSettings.PROVIDERS_OLLAMA -> new OllamaSettingsPrompter(shellPrompter, commandRunner);
            case LlmSettings.PROVIDERS_OPENAI -> new OpenAISettingsPrompter(shellPrompter);
            default -> throw new IllegalStateException("Unsupported AI provider: " + aiProvider);
        };
    }
}
