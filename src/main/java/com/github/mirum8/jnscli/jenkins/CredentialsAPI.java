package com.github.mirum8.jnscli.jenkins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mirum8.jnscli.http.HttpMethod;
import com.github.mirum8.jnscli.http.HttpRequestBuilder;
import com.github.mirum8.jnscli.http.HttpRequestBuilderFactory;
import com.github.mirum8.jnscli.settings.SettingsService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class CredentialsAPI {
    private static final String STORE_PATH = "credentials/store/system/domain/_";
    private static final String LIST_QUERY = "?tree=credentials[id,description,typeName]";
    private static final String USER_PASS_CLASS = "com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl";
    private static final String SECRET_TEXT_CLASS = "org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl";

    private final HttpClient httpClient;
    private final HttpRequestBuilderFactory httpRequestBuilderFactory;
    private final SettingsService settingsService;
    private final CrumbProvider crumbProvider;
    private final ObjectMapper objectMapper;

    public CredentialsAPI(HttpClient httpClient,
                          HttpRequestBuilderFactory httpRequestBuilderFactory,
                          SettingsService settingsService,
                          CrumbProvider crumbProvider) {
        this.httpClient = httpClient;
        this.httpRequestBuilderFactory = httpRequestBuilderFactory;
        this.settingsService = settingsService;
        this.crumbProvider = crumbProvider;
        this.objectMapper = JenkinsApiUtils.createObjectMapper();
    }

    public List<Credential> list() {
        String url = JenkinsApiUtils.joinPath(settingsService.readSettings().server(), STORE_PATH + "/api/json") + LIST_QUERY;
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilderFactory, httpClient);
        CredentialsListResponse body = JenkinsApiUtils.getBody(response, CredentialsListResponse.class, objectMapper);
        return body != null && body.credentials() != null ? body.credentials() : List.of();
    }

    public void createUserPass(String id, String username, String password, String description, String scope) {
        Map<String, Object> credentials = new LinkedHashMap<>();
        credentials.put("scope", scope);
        credentials.put("id", id);
        credentials.put("username", username);
        credentials.put("password", password);
        credentials.put("description", description == null ? "" : description);
        credentials.put("$class", USER_PASS_CLASS);
        postCreate(credentials);
    }

    public void createSecretText(String id, String secret, String description, String scope) {
        Map<String, Object> credentials = new LinkedHashMap<>();
        credentials.put("scope", scope);
        credentials.put("id", id);
        credentials.put("secret", secret);
        credentials.put("description", description == null ? "" : description);
        credentials.put("$class", SECRET_TEXT_CLASS);
        postCreate(credentials);
    }

    public void delete(String id) {
        String url = JenkinsApiUtils.joinPath(settingsService.readSettings().server(),
            STORE_PATH + "/credential/" + encodePathSegment(id) + "/config.xml");
        HttpRequestBuilder builder = httpRequestBuilderFactory.create()
            .url(url)
            .method(HttpMethod.DELETE);
        applyCrumb(builder);
        send(builder.build(), url);
    }

    private void postCreate(Map<String, Object> credentialsPayload) {
        String url = JenkinsApiUtils.joinPath(settingsService.readSettings().server(), STORE_PATH + "/createCredentials");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("", "0");
        payload.put("credentials", credentialsPayload);
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new JenkinsAPIException(e);
        }
        String body = "json=" + URLEncoder.encode(json, StandardCharsets.UTF_8);
        HttpRequestBuilder builder = httpRequestBuilderFactory.create()
            .url(url)
            .method(HttpMethod.POST)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .body(HttpRequest.BodyPublishers.ofString(body));
        applyCrumb(builder);
        send(builder.build(), url);
    }

    private void applyCrumb(HttpRequestBuilder builder) {
        crumbProvider.get().ifPresent(c -> builder.header(c.crumbRequestField(), c.crumb()));
    }

    private static String encodePathSegment(String segment) {
        return URLEncoder.encode(segment, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private void send(HttpRequest request, String url) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new JenkinsAPIException("HTTP: " + response.statusCode() + "; URL: " + url);
            }
        } catch (IOException e) {
            throw new JenkinsAPIException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JenkinsAPIException(e);
        }
    }
}
