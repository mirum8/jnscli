package com.github.mirum8.jnscli.http;

import com.github.mirum8.jnscli.settings.SettingsService;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class HttpRequestBuilder {
    private static final String PATH_DELIMITER = "/";

    private final SettingsService settingsService;
    private final Map<String, String> headers = new HashMap<>();
    private String url;
    private HttpRequest.BodyPublisher bodyPublisher = HttpRequest.BodyPublishers.noBody();
    private HttpMethod method;

    public HttpRequestBuilder(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public HttpRequestBuilder url(String url) {
        this.url = url;
        return this;
    }

    public HttpRequestBuilder path(String path) {
        String hostname = settingsService.readSettings().server();
        if (!path.startsWith(PATH_DELIMITER) && !hostname.endsWith(PATH_DELIMITER)) {
            path = PATH_DELIMITER + path;
        }
        this.url = hostname + path;
        return this;
    }

    public HttpRequestBuilder method(HttpMethod method) {
        this.method = method;
        return this;
    }

    public HttpRequestBuilder body(HttpRequest.BodyPublisher bodyPublisher) {
        this.bodyPublisher = bodyPublisher;
        return this;
    }

    public HttpRequestBuilder header(String key, String value) {
        headers.put(key, value);
        return this;
    }

    public HttpRequest build() {
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
        requestBuilder.timeout(Duration.ofSeconds(10));
        try {
            requestBuilder.uri(new URI(url));
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
        requestBuilder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString((settingsService.readSettings().username() + ":" + settingsService.readSettings().key()).getBytes()));
        requestBuilder.header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36");
        headers.forEach(requestBuilder::header);

        return switch (method) {
            case GET -> requestBuilder.GET().build();
            case POST -> requestBuilder.POST(bodyPublisher).build();
            case PUT -> requestBuilder.PUT(bodyPublisher).build();
            case DELETE -> requestBuilder.DELETE().build();
        };
    }
}
