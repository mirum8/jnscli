package com.github.mirum8.jnscli.ai;

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

    public AiService(SettingsService settingsService, ShellPrompter prompter, AiClientFactory aiClientFactory, AiSettingsPrompterFactory aiSettingsPrompterFactory, ShellPrinter shellPrinter) {
        this.settingsService = settingsService;
        this.prompter = prompter;
        this.aiClientFactory = aiClientFactory;
        this.aiSettingsPrompterFactory = aiSettingsPrompterFactory;
        this.shellPrinter = shellPrinter;
    }

    public void configure() {
        String provider = prompter.promptSelectFromList("Select AI provider", LlmSettings.supportedProviders());
        LlmSettings llmSettings = aiSettingsPrompterFactory.create(provider).promptSettings(provider);
        shellPrinter.println("Settings are saved.");
        settingsService.writeAiSettings(llmSettings);
    }


    public String analyzeLog(String log) {
        return settingsService.readAiSettings()
            .map(aiSettings -> {
                AiClient aiClient = aiClientFactory.create(aiSettings);
                return aiClient.generate(String.format(Templates.ANALYZE_LOG_TEMPLATE, log));
            })
            .orElseThrow(() -> new IllegalStateException("AI settings not configured. Run 'ai configure' command."));
    }

    public void test() {
        settingsService.readAiSettings()
            .map(aiClientFactory::create)
            .ifPresent(aiClient -> shellPrinter.println(aiClient.generate("Who are you?")));
    }
}
