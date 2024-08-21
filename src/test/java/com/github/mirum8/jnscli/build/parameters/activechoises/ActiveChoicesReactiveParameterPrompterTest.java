package com.github.mirum8.jnscli.build.parameters.activechoises;

import com.github.mirum8.jnscli.http.HttpRequestBuilder;
import com.github.mirum8.jnscli.jenkins.WorkflowJob;
import com.github.mirum8.jnscli.settings.Settings;
import com.github.mirum8.jnscli.settings.SettingsService;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ActiveChoicesReactiveParameterPrompterTest {

    private static final String TEST_CRUMB = "testCrumb";
    private static final String RESPONSE_BODY_INIT = "makeStaplerProxy('/$stapler/bound/some-bound','testCrumb')";
    private static final String RESPONSE_BODY_OPTIONS = "[\"choice1\",\"choice2\",\"choice3\"]";
    private static final String RESPONSE_BODY_HTML = "<html>some html content</html>";
    private static final String JOB_URL = "http://example.com/";

    @Mock
    private ShellPrompter shellPrompter;

    @Mock
    private HttpClient httpClient;

    @Mock
    private ActiveChoiceExtractor activeChoiceExtractor;

    @InjectMocks
    private ActiveChoicesReactiveParameterPrompter activeChoicesReactiveParameterPrompter;

    @Mock
    private WorkflowJob job;

    @Mock
    private HttpResponse<String> httpResponse;

    @Mock
    private SettingsService settingsService;

    @InjectMocks
    private HttpRequestBuilder httpRequestBuilder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        settingsService = Mockito.mock(SettingsService.class);
        when(settingsService.readSettings()).thenReturn(new Settings("http://example.com", "user", "test"));
        httpRequestBuilder = new HttpRequestBuilder(settingsService);
        activeChoicesReactiveParameterPrompter = new ActiveChoicesReactiveParameterPrompter(shellPrompter, httpClient, httpRequestBuilder, activeChoiceExtractor);
    }

    @Test
    void testInitCascadeChoiceParameter() throws IOException, InterruptedException {
        ActiveChoice activeChoice = new ActiveChoice("uid", List.of("refParam"), false, null);
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(RESPONSE_BODY_INIT);

        String crumb = activeChoicesReactiveParameterPrompter.initCascadeChoiceParameter(activeChoice);

        assertEquals(TEST_CRUMB, crumb);
        verify(httpClient).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void testGetOptions() throws IOException, InterruptedException {
        ActiveChoice activeChoice = new ActiveChoice("uid", List.of("refParam"), false, null);
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(RESPONSE_BODY_OPTIONS);

        List<String> options = activeChoicesReactiveParameterPrompter.getChoices(activeChoice, TEST_CRUMB);

        assertEquals(List.of("choice1", "choice2", "choice3"), options);
        verify(httpClient).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void testInvokeGroovyScript() throws IOException, InterruptedException {
        ActiveChoice activeChoice = new ActiveChoice("uid", List.of("refParam"), false, null);
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(204);

        assertDoesNotThrow(() -> activeChoicesReactiveParameterPrompter.doUpdate(activeChoice, Map.of("refParam", "refValue"), TEST_CRUMB));
        verify(httpClient).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void testGetHtmlForParsingActiveChoices() throws IOException, InterruptedException {
        when(job.url()).thenReturn(JOB_URL);
        when(httpClient.send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()))).thenReturn(httpResponse);
        when(httpResponse.statusCode()).thenReturn(405);
        when(httpResponse.body()).thenReturn(RESPONSE_BODY_HTML);

        String html = activeChoicesReactiveParameterPrompter.getHtmlForParsingActiveChoices(job);

        assertEquals(RESPONSE_BODY_HTML, html);
        verify(httpClient).send(any(HttpRequest.class), eq(HttpResponse.BodyHandlers.ofString()));
    }

    @Test
    void testApplicableForTypes() {
        Set<String> types = activeChoicesReactiveParameterPrompter.applicableForTypes();

        assertEquals(Set.of("CascadeChoiceParameter"), types);
    }
}
