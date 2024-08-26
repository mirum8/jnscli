package com.github.mirum8.jnscli.jenkins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mirum8.jnscli.http.HttpMethod;
import com.github.mirum8.jnscli.http.HttpRequestBuilder;
import com.github.mirum8.jnscli.settings.Settings;
import com.github.mirum8.jnscli.settings.SettingsService;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Component
public class JenkinsAdapter {
    private static final String BOUNDARY = "ZeeBoundX12345AbCdEf";
    public static final String API_JSON = "/api/json";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final HttpRequestBuilder httpRequestBuilder;
    private final SettingsService settingsService;

    public JenkinsAdapter(HttpClient httpClient,
                          HttpRequestBuilder httpRequestBuilder,
                          SettingsService settingsService) {
        this.httpRequestBuilder = httpRequestBuilder;
        this.httpClient = httpClient;
        this.settingsService = settingsService;
        this.objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public CheckConnectionResult checkConnection(Settings settings) {
        try {
            sendRequest(HttpMethod.GET, settings.server() + API_JSON);
            return new CheckConnectionResult(CheckConnectionResult.Status.SUCCESS, "Connection to Jenkins server " + settings.server() + " was successful");
        } catch (Exception e) {
            return new CheckConnectionResult(CheckConnectionResult.Status.FAILURE, e.getMessage());
        }
    }

    public WorkflowRun getJobBuildDescription(String jobUrl, int buildNumber) {
        String url = jobUrl + "/" + buildNumber + "/wfapi/describe";
        HttpResponse<String> response = sendRequest(HttpMethod.GET, url);
        return getBody(response, WorkflowRun.class);
    }

    public BuildInfo getJobBuildInfo(String jobUrl, int buildNumber) {
        String url = jobUrl + "/" + buildNumber + API_JSON;
        HttpResponse<String> response = sendRequest(HttpMethod.GET, url);
        return getBody(response, BuildInfo.class);
    }

    public List<Run> getJobRuns(String jobUrl) {
        String url = jobUrl.endsWith("/") ? jobUrl + "wfapi/runs" : jobUrl + "/wfapi/runs";
        HttpResponse<String> response = sendRequest(HttpMethod.GET, url);
        try {
            return objectMapper.readValue(response.body(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new JenkinsAdapterException(e);
        }
    }

    public List<Job> getJobs() {
        Settings settings = settingsService.readSettings();
        HttpResponse<String> response = sendRequest(HttpMethod.GET, settings.server() + "/view/all/api/json");
        ViewAll viewAll = getBody(response, ViewAll.class);
        return viewAll != null && viewAll.jobs() != null ? viewAll.jobs() : List.of();
    }

    public QueueItemLocation runJob(String jobUrl) {
        String url = jobUrl + "/build";
        HttpResponse<String> response = sendRequest(HttpMethod.POST, url);
        return getQueueItemLocation(response);
    }

    public QueueItemLocation runJob(String jobUrl, List<String> parameters) {
        String url = jobUrl + "/buildWithParameters";
        if (parameters != null && !parameters.isEmpty()) {
            url += "?" + String.join("&", parameters);
        }
        HttpResponse<String> response = sendRequest(HttpMethod.POST, url);
        return getQueueItemLocation(response);
    }

    public QueueItemLocation runJobWithFileParam(String jobUrl, String fileParamName, Path filePath, List<String> parameters) {
        String url = jobUrl + "/buildWithParameters";

        if (parameters != null && !parameters.isEmpty()) {
            url += "?" + String.join("&", parameters);
        }

        try {
            HttpRequest request = httpRequestBuilder
                .url(url)
                .method(HttpMethod.POST)
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .body(buildMultipartBody(fileParamName, filePath))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200 && response.statusCode() != 201) {
                throw new JenkinsAdapterException("Failed to run job with file parameter, status code: " + response.statusCode());
            }
            return getQueueItemLocation(response);
        } catch (IOException e) {
            throw new JenkinsAdapterException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JenkinsAdapterException(e);
        }
    }

    private HttpRequest.BodyPublisher buildMultipartBody(String fileParamName, Path filePath) throws IOException {
        String filename = filePath.getFileName().toString();
        String formData = "--" + BOUNDARY +
            "\r\nContent-Disposition: form-data; name=\"" + fileParamName + "\"; filename=\"" + filename +
            "\"\r\nContent-Type: application/octet-stream\r\n\r\n";

        byte[] fileBytes = Files.readAllBytes(filePath);
        byte[] endBoundary = ("\r\n--" + BOUNDARY + "--").getBytes(StandardCharsets.UTF_8);

        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            outputStream.write(formData.getBytes(StandardCharsets.UTF_8));
            outputStream.write(fileBytes);
            outputStream.write(endBoundary);
            return HttpRequest.BodyPublishers.ofByteArray(outputStream.toByteArray());
        }
    }

    public WorkflowJob getWorkflowJob(String jobUrl) {
        String url = jobUrl + API_JSON;
        HttpResponse<String> response = sendRequest(HttpMethod.GET, url);
        return getBody(response, WorkflowJob.class);
    }

    public StageDescription getStageDescription(String jobUrl, long buildNumber, String stageId) {
        String url = jobUrl + "/" + buildNumber + "/execution/node/" + stageId + "/wfapi/describe";
        HttpResponse<String> response = sendRequest(HttpMethod.GET, url);
        return getBody(response, StageDescription.class);
    }

    public String getConsoleText(String jobUrl, int buildNumber) {
        String url = jobUrl + "/" + buildNumber + "/consoleText";
        HttpResponse<String> response = sendRequest(HttpMethod.GET, url);
        return response.body();
    }

    public NodeLog getNodeLog(String jobUrl, long buildNumber, String nodeId) {
        String url = jobUrl + "/" + buildNumber + "/execution/node/" + nodeId + "/wfapi/log";
        HttpResponse<String> response = sendRequest(HttpMethod.GET, url);
        return getBody(response, NodeLog.class);
    }

    public void abortJob(String jobUrl, int buildNumber) {
        String url = jobUrl + "/" + buildNumber + "/stop";
        sendRequest(HttpMethod.POST, url);
    }

    public Folder getFolderJobs(String folderUrl) {
        String url = folderUrl + API_JSON;
        HttpResponse<String> response = sendRequest(HttpMethod.GET, url);
        return getBody(response, Folder.class);
    }

    public QueueItem getQueueItem(String url) {
        HttpResponse<String> response = sendRequest(HttpMethod.GET, url + API_JSON);
        return getBody(response, QueueItem.class);
    }

    public ProgressiveConsoleText getProgressiveConsoleText(String jobUrl, int buildNumber, Long start) {
        StringBuilder urlBuilder = new StringBuilder(jobUrl)
            .append("/").append(buildNumber)
            .append("/logText/progressiveText");

        if (start != null) {
            urlBuilder.append("?start=").append(start);
        }

        String url = urlBuilder.toString();
        HttpResponse<String> response = sendRequest(HttpMethod.GET, url);

        boolean hasMoreData = Boolean.parseBoolean(response.headers().firstValue("X-More-Data").orElse("false"));
        long nextStart = Long.parseLong(response.headers().firstValue("X-Text-Size").orElse("0"));
        return new ProgressiveConsoleText(response.body(), hasMoreData, nextStart);
    }


    private HttpResponse<String> sendRequest(HttpMethod httpMethod, String url) {
        url = url.replace(" ", "%20");
        try {
            HttpRequest request = httpRequestBuilder
                .url(url)
                .method(httpMethod)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new JenkinsAdapterException("HTTP: " + response.statusCode() + "; URL: " + url);
            }
            return response;
        } catch (IOException e) {
            throw new JenkinsAdapterException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JenkinsAdapterException(e);
        }
    }

    private <T> T getBody(HttpResponse<String> response, Class<T> clazz) {
        try {
            return objectMapper.readValue(response.body(), clazz);
        } catch (JsonProcessingException e) {
            throw new JenkinsAdapterException(e);
        }
    }

    private QueueItemLocation getQueueItemLocation(HttpResponse<String> response) {
        String location = response.headers().firstValue("Location").orElse(null);
        if (location == null) {
            throw new JenkinsAdapterException("No Location header in response");
        }
        return new QueueItemLocation(location);
    }
}
