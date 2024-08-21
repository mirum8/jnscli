package com.github.mirum8.jnscli.jenkins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mirum8.jnscli.http.HttpRequestBuilder;
import com.github.mirum8.jnscli.settings.Settings;
import com.github.mirum8.jnscli.settings.SettingsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class JenkinsAdapterTest {
    private static final String BASE_URL = "http://localhost";
    private static final String JOB_URL = BASE_URL + "/job/test";
    private static final String API_JSON = "/api/json";

    private JenkinsAdapter jenkinsAdapter;
    private HttpClient httpClient;
    private Settings settings;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        settings = new Settings(BASE_URL, "user", "key");
        httpClient = mock(HttpClient.class);
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.readSettings()).thenReturn(settings);
        objectMapper = new ObjectMapper();
        jenkinsAdapter = new JenkinsAdapter(
            httpClient,
            new HttpRequestBuilder(settingsService),
            settingsService
        );
    }

    @Nested
    class ConnectionTests {
        @Test
        void shouldReturnSuccessWhenConnectionIsSuccessful() throws IOException, InterruptedException {
            mockHttpResponse(200, "{\"_class\":\"hudson.model.AllView\",\"jobs\":[]}");

            CheckConnectionResult result = jenkinsAdapter.checkConnection(settings);

            assertThat(result.status()).isEqualTo(CheckConnectionResult.Status.SUCCESS);
            assertThat(result.message()).contains("Connection to Jenkins server " + BASE_URL + " was successful");
        }

        @Test
        void shouldReturnFailureWhenConnectionFails() throws IOException, InterruptedException {
            mockHttpResponse(404, "Not Found");

            CheckConnectionResult result = jenkinsAdapter.checkConnection(settings);

            assertThat(result.status()).isEqualTo(CheckConnectionResult.Status.FAILURE);
            assertThat(result.message()).contains("HTTP: 404");
        }
    }

    @Nested
    class JobOperations {
        @Test
        void shouldRetrieveJobBuildDescription() throws IOException, InterruptedException {
            String json = "{\"id\":\"1\",\"name\":\"Build\",\"status\":\"SUCCESS\"}";
            mockHttpResponse(200, json);

            WorkflowRun result = jenkinsAdapter.getJobBuildDescription(JOB_URL, 1);

            assertThat(result).isEqualTo(objectMapper.readValue(json, WorkflowRun.class));
        }

        @Test
        void shouldRetrieveListOfJobs() throws IOException, InterruptedException {
            String json = "{\"jobs\":[{\"name\":\"job1\"},{\"name\":\"job2\"}]}";
            mockHttpResponse(200, json);

            List<Job> result = jenkinsAdapter.getJobs();

            assertThat(result)
                .hasSize(2)
                .extracting(Job::name)
                .containsExactly("job1", "job2");
        }

        @Test
        void shouldRunJobWithoutParameters() throws IOException, InterruptedException {
            mockHttpResponse(201, "", "Location", "http://localhost/queue/item/123/");

            QueueItemLocation result = jenkinsAdapter.runJob(JOB_URL);

            verify(httpClient).send(argThat(request ->
                request.method().equalsIgnoreCase("POST") &&
                    request.uri().toString().equals(JOB_URL + "/build")
            ), any());
            assertThat(result.url()).isEqualTo("http://localhost/queue/item/123/");
        }

        @ParameterizedTest
        @CsvSource({
            "param1=value1",
            "param1=value1,param2=value2"
        })
        void shouldRunJobWithParameters(String params) throws IOException, InterruptedException {
            mockHttpResponse(201, "", "Location", "http://localhost/queue/item/123/");

            QueueItemLocation result = jenkinsAdapter.runJob(JOB_URL, List.of(params.split(",")));

            verify(httpClient).send(argThat(request ->
                request.method().equalsIgnoreCase("POST") &&
                    request.uri().toString().equals(JOB_URL + "/buildWithParameters?" + params)
            ), any());
            assertThat(result.url()).isEqualTo("http://localhost/queue/item/123/");
        }

        @Test
        void shouldRunJobWithFileParameter() throws IOException, InterruptedException {
            mockHttpResponse(201, "", "Location", "http://localhost/queue/item/123/");

            Path tempFile = Files.createTempFile("test", ".txt");
            try {
                Files.writeString(tempFile, "Test content");

                QueueItemLocation result = jenkinsAdapter.runJobWithFileParam(JOB_URL, "file", tempFile, List.of("param1=value1"));

                ArgumentCaptor<HttpRequest> captor = ArgumentCaptor.forClass(HttpRequest.class);
                verify(httpClient).send(captor.capture(), any());

                HttpRequest request = captor.getValue();
                assertThat(request.method()).isEqualTo("POST");
                assertThat(request.uri()).hasToString(JOB_URL + "/buildWithParameters?param1=value1");
                assertThat(request.headers().firstValue("Content-Type"))
                    .isPresent()
                    .get().asString()
                    .startsWith("multipart/form-data; boundary=");

                assertThat(result.url()).isEqualTo("http://localhost/queue/item/123/");
            } finally {
                Files.deleteIfExists(tempFile);
            }
        }
    }

    @Nested
    class JobInformationRetrieval {
        @Test
        void shouldRetrieveWorkflowJob() throws IOException, InterruptedException {
            String json = "{\"name\":\"job1\",\"url\":\"" + JOB_URL + "\"}";
            mockHttpResponse(200, json);

            WorkflowJob result = jenkinsAdapter.getWorkflowJob(JOB_URL);

            assertThat(result).isEqualTo(objectMapper.readValue(json, WorkflowJob.class));
        }

        @Test
        void shouldRetrieveStageDescription() throws IOException, InterruptedException {
            String json = "{\"id\":\"1\",\"name\":\"Stage 1\",\"status\":\"SUCCESS\",\"stageFlowNodes\":[]}";
            mockHttpResponse(200, json);

            StageDescription result = jenkinsAdapter.getStageDescription(JOB_URL, 1, "1");

            assertThat(result).isEqualTo(new StageDescription("1", "SUCCESS", List.of()));
            verify(httpClient).send(argThat(request ->
                request.method().equalsIgnoreCase("GET") &&
                    request.uri().toString().equals(JOB_URL + "/1/execution/node/1/wfapi/describe")
            ), any());
        }

        @Test
        void shouldRetrieveConsoleText() throws IOException, InterruptedException {
            String consoleText = "Build log...";
            mockHttpResponse(200, consoleText);

            String result = jenkinsAdapter.getConsoleText(JOB_URL, 1);

            assertThat(result).isEqualTo(consoleText);
        }

        @Test
        void shouldRetrieveNodeLog() throws IOException, InterruptedException {
            String json = "{\"text\":\"Log text...\"}";
            mockHttpResponse(200, json);

            NodeLog result = jenkinsAdapter.getNodeLog(JOB_URL, 1, "1");

            assertThat(result).isEqualTo(objectMapper.readValue(json, NodeLog.class));
        }

        @Test
        void shouldRetrieveJobRuns() throws IOException, InterruptedException {
            String json = """
                [
                  {
                    "id": "1",
                    "name": "job1",
                    "status": "SUCCESS",
                    "stages": [
                      {
                        "id": "1",
                        "name": "stage1",
                        "status": "SUCCESS"
                      }
                    ]
                  },
                  {
                    "id": "2",
                    "name": "job2",
                    "status": "FAILED",
                    "stages": [
                      {
                        "id": "2",
                        "name": "stage2",
                        "status": "FAILED"
                      }
                    ]
                  }
                ]
                """;
            mockHttpResponse(200, json);

            List<Run> jobRuns = jenkinsAdapter.getJobRuns(JOB_URL);

            assertThat(jobRuns)
                .hasSize(2)
                .containsExactly(
                    new Run(1, "job1", Status.SUCCESS, List.of(new Run.Stage("1", "stage1", Status.SUCCESS))),
                    new Run(2, "job2", Status.FAILED, List.of(new Run.Stage("2", "stage2", Status.FAILED)))
                );

            verify(httpClient).send(argThat(request ->
                request.method().equalsIgnoreCase("GET") &&
                    request.uri().toString().equals(JOB_URL + "/wfapi/runs")
            ), any());
        }
    }

    @Test
    void shouldAbortJob() throws IOException, InterruptedException {
        mockHttpResponse(200, "");

        jenkinsAdapter.abortJob(JOB_URL, 1);

        verify(httpClient).send(argThat(request ->
            request.method().equalsIgnoreCase("POST") &&
                request.uri().toString().equals(JOB_URL + "/1/stop")
        ), any());
    }

    @Test
    void shouldRetrieveFolderJobs() throws IOException, InterruptedException {
        String json = "{\"jobs\":[{\"name\":\"job1\"},{\"name\":\"job2\"}]}";
        mockHttpResponse(200, json);

        Folder result = jenkinsAdapter.getFolderJobs(BASE_URL + "/job/folder1");

        assertThat(result).isEqualTo(objectMapper.readValue(json, Folder.class));
    }

    @Test
    void shouldThrowExceptionOnHttpError() throws IOException, InterruptedException {
        mockHttpResponse(404, "Not Found");

        assertThatThrownBy(() -> jenkinsAdapter.getJobs())
            .isInstanceOf(JenkinsAdapterException.class)
            .hasMessageContaining("HTTP: 404");
    }

    private void mockHttpResponse(int statusCode, String body) throws IOException, InterruptedException {
        mockHttpResponse(statusCode, body, null, null);
    }

    private void mockHttpResponse(int statusCode, String body, String headerName, String headerValue) throws IOException, InterruptedException {
        HttpResponse<String> response = mock(HttpResponse.class);
        when(response.statusCode()).thenReturn(statusCode);
        when(response.body()).thenReturn(body);
        if (headerName != null && headerValue != null) {
            when(response.headers()).thenReturn(HttpHeaders.of(Map.of(headerName, List.of(headerValue)), (s1, s2) -> true));
        }
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
    }
}
