package com.github.mirum8.jnscli.jenkins;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mirum8.jnscli.http.HttpRequestBuilderFactory;
import com.github.mirum8.jnscli.settings.Settings;
import com.github.mirum8.jnscli.settings.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CredentialsAPITest {
    private static final String BASE_URL = "http://localhost";
    private static final String CREATE_URL = BASE_URL + "/credentials/store/system/domain/_/createCredentials";

    private CredentialsAPI api;
    private HttpClient httpClient;
    private CrumbProvider crumbProvider;

    @BeforeEach
    void setUp() {
        httpClient = mock(HttpClient.class);
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.readSettings()).thenReturn(new Settings(BASE_URL, "user", "key"));
        crumbProvider = mock(CrumbProvider.class);
        when(crumbProvider.get()).thenReturn(Optional.empty());
        api = new CredentialsAPI(
            httpClient,
            new HttpRequestBuilderFactory(settingsService),
            settingsService,
            crumbProvider
        );
    }

    @Test
    void listParsesCredentialsFromJsonResponse() throws Exception {
        mockResponse(200,
            "{\"credentials\":[{\"id\":\"deploy-bot\",\"description\":\"CI bot\",\"typeName\":\"Username with password\"}," +
                "{\"id\":\"api-key\",\"description\":\"\",\"typeName\":\"Secret text\"}]}");

        List<Credential> result = api.list();

        assertThat(result)
            .extracting(Credential::id)
            .containsExactly("deploy-bot", "api-key");
    }

    @Test
    void createUserPassPostsFormEncodedJsonWithUserPassClass() throws Exception {
        mockResponse(200, "");

        api.createUserPass("deploy-bot", "ci", "p@ss", "CI bot", "GLOBAL");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        HttpRequest request = captor.getValue();
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.uri()).hasToString(CREATE_URL);
        assertThat(request.headers().firstValue("Content-Type"))
            .hasValue("application/x-www-form-urlencoded");

        JsonNode payload = decodeFormJson(request);
        assertThat(payload.get("").asText()).isEqualTo("0");
        JsonNode credentials = payload.get("credentials");
        assertThat(credentials.get("$class").asText())
            .isEqualTo("com.cloudbees.plugins.credentials.impl.UsernamePasswordCredentialsImpl");
        assertThat(credentials.get("scope").asText()).isEqualTo("GLOBAL");
        assertThat(credentials.get("id").asText()).isEqualTo("deploy-bot");
        assertThat(credentials.get("username").asText()).isEqualTo("ci");
        assertThat(credentials.get("password").asText()).isEqualTo("p@ss");
        assertThat(credentials.get("description").asText()).isEqualTo("CI bot");
    }

    @Test
    void createSecretTextPostsFormEncodedJsonWithStringClass() throws Exception {
        mockResponse(200, "");

        api.createSecretText("api-key", "shhh", null, "GLOBAL");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        JsonNode payload = decodeFormJson(captor.getValue());
        JsonNode credentials = payload.get("credentials");
        assertThat(credentials.get("$class").asText())
            .isEqualTo("org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl");
        assertThat(credentials.get("secret").asText()).isEqualTo("shhh");
        assertThat(credentials.get("description").asText()).isEmpty();
    }

    @Test
    void deleteIssuesDeleteAgainstConfigXmlPath() throws Exception {
        mockResponse(200, "");

        api.delete("deploy-bot");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        HttpRequest request = captor.getValue();
        assertThat(request.method()).isEqualTo("DELETE");
        assertThat(request.uri())
            .hasToString(BASE_URL + "/credentials/store/system/domain/_/credential/deploy-bot/config.xml");
    }

    @Test
    void deleteUsesRfc3986PathSegmentEncoding() throws Exception {
        mockResponse(200, "");

        api.delete("a b/c");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertThat(captor.getValue().uri().getRawPath())
            .endsWith("/credential/a%20b%2Fc/config.xml");
    }

    @Test
    void includesJenkinsCrumbHeaderWhenCrumbProviderReturnsValue() throws Exception {
        when(crumbProvider.get()).thenReturn(Optional.of(new Crumb("abc123", "Jenkins-Crumb")));
        mockResponse(200, "");

        api.delete("any-id");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertThat(captor.getValue().headers().firstValue("Jenkins-Crumb")).hasValue("abc123");
    }

    @Test
    void omitsCrumbHeaderWhenCrumbProviderReturnsEmpty() throws Exception {
        mockResponse(200, "");

        api.createSecretText("api-key", "shhh", null, "GLOBAL");

        ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
        verify(httpClient).send(captor.capture(), any());
        assertThat(captor.getValue().headers().firstValue("Jenkins-Crumb")).isEmpty();
    }

    private void mockResponse(int status, String body) throws IOException, InterruptedException {
        @SuppressWarnings("unchecked")
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(status);
        when(response.body()).thenReturn(body);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }

    private JsonNode decodeFormJson(HttpRequest request) throws IOException {
        String raw = bodyAsString(request);
        assertThat(raw).startsWith("json=");
        String json = URLDecoder.decode(raw.substring("json=".length()), StandardCharsets.UTF_8);
        return new ObjectMapper().readTree(json);
    }

    private String bodyAsString(HttpRequest request) {
        StringBuilder sb = new StringBuilder();
        request.bodyPublisher().ifPresent(publisher -> publisher.subscribe(new java.util.concurrent.Flow.Subscriber<>() {
            @Override
            public void onSubscribe(java.util.concurrent.Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(java.nio.ByteBuffer item) {
                byte[] bytes = new byte[item.remaining()];
                item.get(bytes);
                sb.append(new String(bytes, StandardCharsets.UTF_8));
            }

            @Override
            public void onError(Throwable throwable) {
                throw new IllegalStateException(throwable);
            }

            @Override
            public void onComplete() {
                // body fully drained into sb via onNext; nothing else to do
            }
        }));
        return sb.toString();
    }
}
