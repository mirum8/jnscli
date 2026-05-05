package com.github.mirum8.jnscli.ai;

import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.runner.SpinnerFactory;
import com.github.mirum8.jnscli.shell.Messages;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AiSettingsPrompterFactory {
    private final ShellPrompter shellPrompter;
    private final CommandRunner commandRunner;
    private final SpinnerFactory spinnerFactory;
    private final Messages messages;
    private final String ollamaModel;

    public AiSettingsPrompterFactory(ShellPrompter shellPrompter,
                                     CommandRunner commandRunner,
                                     SpinnerFactory spinnerFactory,
                                     Messages messages,
                                     @Value("${app.ai.ollama.model}") String ollamaModel) {
        this.shellPrompter = shellPrompter;
        this.commandRunner = commandRunner;
        this.spinnerFactory = spinnerFactory;
        this.messages = messages;
        this.ollamaModel = ollamaModel;
    }

    public AiSettingsPrompter create(String aiProvider) {
        return switch (aiProvider) {
            case LlmSettings.PROVIDERS_OLLAMA ->
                    new OllamaSettingsPrompter(shellPrompter, commandRunner, spinnerFactory, messages, ollamaModel);
            case LlmSettings.PROVIDERS_OPENAI -> new OpenAISettingsPrompter(shellPrompter);
            default -> throw new IllegalStateException("Unsupported AI provider: " + aiProvider);
        };
    }
}
