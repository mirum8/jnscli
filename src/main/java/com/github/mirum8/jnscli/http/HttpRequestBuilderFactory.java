package com.github.mirum8.jnscli.http;

import com.github.mirum8.jnscli.settings.SettingsService;
import org.springframework.stereotype.Component;

@Component
public class HttpRequestBuilderFactory {
    private final SettingsService settingsService;

    public HttpRequestBuilderFactory(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public HttpRequestBuilder create() {
        return new HttpRequestBuilder(settingsService);
    }
}
