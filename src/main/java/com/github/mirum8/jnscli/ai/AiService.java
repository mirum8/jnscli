package com.github.mirum8.jnscli.ai;

import com.github.mirum8.jnscli.runner.CommandParameters;
import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.runner.Result;
import com.github.mirum8.jnscli.runner.Spinner;
import com.github.mirum8.jnscli.settings.SettingsService;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import org.springframework.stereotype.Component;

@Component
public class AiService {
    private final SettingsService settingsService;
    private final ShellPrompter prompter;
    private final AiClientFactory aiClientFactory;
    private final AiSettingsPrompterFactory aiSettingsPrompterFactory;
    private final ShellPrinter shellPrinter;
    private final CommandRunner commandRunner;

    public AiService(SettingsService settingsService, ShellPrompter prompter, AiClientFactory aiClientFactory, AiSettingsPrompterFactory aiSettingsPrompterFactory, ShellPrinter shellPrinter, CommandRunner commandRunner) {
        this.settingsService = settingsService;
        this.prompter = prompter;
        this.aiClientFactory = aiClientFactory;
        this.aiSettingsPrompterFactory = aiSettingsPrompterFactory;
        this.shellPrinter = shellPrinter;
        this.commandRunner = commandRunner;
    }

    public void configure() {
        String provider = prompter.promptSelectFromList("Select AI provider", LlmSettings.supportedProviders());
        LlmSettings llmSettings = aiSettingsPrompterFactory.create(provider).promptSettings();
        shellPrinter.println("Settings are saved.");
        settingsService.writeAiSettings(llmSettings);
    }

    public String analyzeLog(String log) {
        Result<String> result = commandRunner.run(
            () -> settingsService.readAiSettings()
                .map(aiSettings -> {
                    AiClient aiClient = aiClientFactory.create(aiSettings);
                    return aiClient.generate(String.format(Templates.ANALYZE_LOG_TEMPLATE, log));
                })
                .orElse(null),
            CommandParameters.<String>builder()
                .withProgressBar(Spinner.builder("Analyzing log")
                    .errorMessage("Log analysis failed")
                    .build())
                .withTimeout(120)
                .build()
        );

        return switch (result) {
            case Result.Success<String> success -> success.value();
            case Result.Failure<?> ignored ->
                throw new IllegalStateException("AI settings not configured or log analysis failed. Run 'ai configure' command.");
        };
    }

    public void test() {
        Result<String> result = commandRunner.run(
            () -> settingsService.readAiSettings()
                .map(aiClientFactory::create)
                .map(aiClient -> aiClient.generate("Who are you?"))
                .orElse(null),
            CommandParameters.<String>builder()
                .withProgressBar(Spinner.builder("Testing AI connection")
                    .completeMessage("Connection test passed")
                    .build())
                .withTimeout(60)
                .build()
        );

        switch (result) {
            case Result.Success<?> success -> shellPrinter.println("AI response: " + success.value());
            case Result.Failure<?> ignored ->
                shellPrinter.println("AI settings not configured or test failed. Run 'ai configure' command.");
        }
    }
}
