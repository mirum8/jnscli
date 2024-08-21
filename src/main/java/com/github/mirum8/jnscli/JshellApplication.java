package com.github.mirum8.jnscli;

import com.github.mirum8.jnscli.settings.SettingsProperties;
import org.jline.utils.AttributedString;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.shell.command.annotation.CommandScan;
import org.springframework.shell.jline.PromptProvider;

@SpringBootApplication
@CommandScan
@EnableConfigurationProperties(SettingsProperties.class)
public class JshellApplication implements PromptProvider {

    public static void main(String[] args) {
        SpringApplication.run(JshellApplication.class, args);
    }

    @Override
    public AttributedString getPrompt() {
        return new AttributedString("jns:>");
    }
}
