package com.github.mirum8.jnscli.jenkins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mirum8.jnscli.http.HttpMethod;
import com.github.mirum8.jnscli.http.HttpRequestBuilderFactory;
import com.github.mirum8.jnscli.settings.SettingsService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

@Component
public class CrumbProvider {
    private final HttpClient httpClient;
    private final HttpRequestBuilderFactory httpRequestBuilderFactory;
    private final SettingsService settingsService;
    private final ObjectMapper objectMapper;

    private boolean cached;
    private Optional<Crumb> cachedCrumb = Optional.empty();

    public CrumbProvider(HttpClient httpClient,
                         HttpRequestBuilderFactory httpRequestBuilderFactory,
                         SettingsService settingsService) {
        this.httpClient = httpClient;
        this.httpRequestBuilderFactory = httpRequestBuilderFactory;
        this.settingsService = settingsService;
        this.objectMapper = JenkinsApiUtils.createObjectMapper();
    }

    public synchronized Optional<Crumb> get() {
        if (cached) {
            return cachedCrumb;
        }
        cachedCrumb = fetch();
        cached = true;
        return cachedCrumb;
    }

    private Optional<Crumb> fetch() {
        String url = JenkinsApiUtils.joinPath(settingsService.readSettings().server(), "crumbIssuer/api/json");
        try {
            HttpRequest request = httpRequestBuilderFactory.create()
                .url(url)
                .method(HttpMethod.GET)
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 404) {
                return Optional.empty();
            }
            if (response.statusCode() >= 400) {
                throw new JenkinsAPIException("HTTP: " + response.statusCode() + "; URL: " + url);
            }
            return Optional.of(objectMapper.readValue(response.body(), Crumb.class));
        } catch (IOException e) {
            throw new JenkinsAPIException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JenkinsAPIException(e);
        }
    }
}
