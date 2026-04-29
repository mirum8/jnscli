package com.github.mirum8.jnscli.ai;

import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.runner.SpinnerFactory;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import com.github.mirum8.jnscli.shell.Symbols;
import com.github.mirum8.jnscli.shell.Theme;
import org.springframework.stereotype.Component;

@Component
public class AiSettingsPrompterFactory {
    private final ShellPrompter shellPrompter;
    private final CommandRunner commandRunner;
    private final SpinnerFactory spinnerFactory;
    private final Theme theme;
    private final Symbols symbols;

    public AiSettingsPrompterFactory(ShellPrompter shellPrompter, CommandRunner commandRunner, SpinnerFactory spinnerFactory, Theme theme, Symbols symbols) {
        this.shellPrompter = shellPrompter;
        this.commandRunner = commandRunner;
        this.spinnerFactory = spinnerFactory;
        this.theme = theme;
        this.symbols = symbols;
    }

    public AiSettingsPrompter create(String aiProvider) {
        return switch (aiProvider) {
            case LlmSettings.PROVIDERS_OLLAMA -> new OllamaSettingsPrompter(shellPrompter, commandRunner, spinnerFactory, theme, symbols);
            case LlmSettings.PROVIDERS_OPENAI -> new OpenAISettingsPrompter(shellPrompter);
            default -> throw new IllegalStateException("Unsupported AI provider: " + aiProvider);
        };
    }
}
