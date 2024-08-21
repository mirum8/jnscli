package com.github.mirum8.jnscli.settings;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("app.settings")
public record SettingsProperties(
    String directory
) {
}
