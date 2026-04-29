package com.github.mirum8.jnscli.ai;

import com.github.mirum8.jnscli.runner.CommandParameters;
import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.runner.Result;
import com.github.mirum8.jnscli.runner.SpinnerFactory;
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
    private final SpinnerFactory spinnerFactory;

    public AiService(SettingsService settingsService, ShellPrompter prompter, AiClientFactory aiClientFactory, AiSettingsPrompterFactory aiSettingsPrompterFactory, ShellPrinter shellPrinter, CommandRunner commandRunner, SpinnerFactory spinnerFactory) {
        this.settingsService = settingsService;
        this.prompter = prompter;
        this.aiClientFactory = aiClientFactory;
        this.aiSettingsPrompterFactory = aiSettingsPrompterFactory;
        this.shellPrinter = shellPrinter;
        this.commandRunner = commandRunner;
        this.spinnerFactory = spinnerFactory;
    }

    public void configure() {
        String provider = prompter.promptSelectFromList("Select AI provider", LlmSettings.supportedProviders());
        LlmSettings llmSettings = aiSettingsPrompterFactory.create(provider).promptSettings();
        shellPrinter.println("Settings are saved.");
        settingsService.writeAiSettings(llmSettings);
    }

    public String analyzeLog(String log) {
        Result<String> result = commandRunner.callWithSpinner("Analyzing log",
            () -> settingsService.readAiSettings()
                .map(aiSettings -> {
                    AiClient aiClient = aiClientFactory.create(aiSettings);
                    return aiClient.generate(String.format(Templates.ANALYZE_LOG_TEMPLATE, log));
                }).orElse(null));

        return switch (result) {
            case Result.Success<String>(String value) -> value;
            case Result.Failure<?> ignored ->
                throw new IllegalStateException("AI settings not configured or log analysis failed. Run 'ai configure' command.");
        };
    }

    public void test() {
        Result<String> result = commandRunner.call(
            () -> settingsService.readAiSettings()
                .map(aiClientFactory::create)
                .map(aiClient -> aiClient.generate("Who are you?"))
                .orElse(null),
            CommandParameters.<String>builder()
                .withProgressBar(spinnerFactory.builder("Testing AI connection")
                    .completeMessage("Connection test passed")
                    .build())
                .withTimeout(60)
                .build()
        );

        switch (result) {
            case Result.Success<?>(Object value) -> shellPrinter.println("AI response: " + value);
            case Result.Failure<?> ignored ->
                shellPrinter.println("AI settings not configured or test failed. Run 'ai configure' command.");
        }
    }
}
