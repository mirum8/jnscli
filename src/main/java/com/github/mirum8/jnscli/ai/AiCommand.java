package com.github.mirum8.jnscli.ai;

import org.springframework.shell.command.annotation.Command;
import org.springframework.stereotype.Component;

@Command(command = "ai", description = "AI commands")
@Component
public class AiCommand {
    private final AiService aiService;

    public AiCommand(AiService aiService) {
        this.aiService = aiService;
    }

    @Command(command = "configure", description = "Configure AI")
    public void configure() {
        aiService.configure();
    }

    @Command(command = "test", description = "Test AI")
    public void test() {
        aiService.test();
    }
}
