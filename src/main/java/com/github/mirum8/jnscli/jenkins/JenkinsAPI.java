package com.github.mirum8.jnscli.jenkins;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mirum8.jnscli.http.HttpMethod;
import com.github.mirum8.jnscli.http.HttpRequestBuilderFactory;
import com.github.mirum8.jnscli.settings.Settings;
import com.github.mirum8.jnscli.settings.SettingsService;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;

@Component
public class JenkinsAPI {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final HttpRequestBuilderFactory httpRequestBuilderFactory;
    private final SettingsService settingsService;

    public JenkinsAPI(HttpClient httpClient,
                      HttpRequestBuilderFactory httpRequestBuilderFactory,
                      SettingsService settingsService) {
        this.httpRequestBuilderFactory = httpRequestBuilderFactory;
        this.httpClient = httpClient;
        this.settingsService = settingsService;
        this.objectMapper = JenkinsApiUtils.createObjectMapper();
    }

    public CheckConnectionResult checkConnection(Settings settings) {
        try {
            JenkinsApiUtils.sendRequest(HttpMethod.GET, settings.server() + JenkinsApiUtils.API_JSON, httpRequestBuilderFactory, httpClient);
            return new CheckConnectionResult(CheckConnectionResult.Status.SUCCESS, "Connection to Jenkins server " + settings.server() + " was successful");
        } catch (Exception e) {
            return new CheckConnectionResult(CheckConnectionResult.Status.FAILURE, e.getMessage());
        }
    }

    public BuildInfo getJobBuildInfo(String jobUrl, int buildNumber) {
        String url = jobUrl + "/" + buildNumber + JenkinsApiUtils.API_JSON;
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilderFactory, httpClient);
        return JenkinsApiUtils.getBody(response, BuildInfo.class, objectMapper);
    }

    public List<Job> getJobs() {
        Settings settings = settingsService.readSettings();
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, settings.server() + "/view/all/api/json", httpRequestBuilderFactory, httpClient);
        ViewAll viewAll = JenkinsApiUtils.getBody(response, ViewAll.class, objectMapper);
        return viewAll != null && viewAll.jobs() != null ? viewAll.jobs() : List.of();
    }

    public QueueItemLocation runJob(String jobUrl) {
        String url = jobUrl + "/build";
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.POST, url, httpRequestBuilderFactory, httpClient);
        return JenkinsApiUtils.getQueueItemLocation(response);
    }

    public QueueItemLocation runJob(String jobUrl, List<String> parameters) {
        String url = JenkinsApiUtils.buildParameterizedUrl(jobUrl, parameters);
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.POST, url, httpRequestBuilderFactory, httpClient);
        return JenkinsApiUtils.getQueueItemLocation(response);
    }

    public QueueItemLocation runJobWithFileParam(String jobUrl, String fileParamName, Path filePath, List<String> parameters) {
        return JenkinsApiUtils.runJobWithFileParam(jobUrl, fileParamName, filePath, parameters, httpRequestBuilderFactory, httpClient);
    }

    public void abortJob(String jobUrl, int buildNumber) {
        String url = jobUrl + "/" + buildNumber + "/stop";
        JenkinsApiUtils.sendRequest(HttpMethod.POST, url, httpRequestBuilderFactory, httpClient);
    }

    public WorkflowJob getWorkflowJob(String jobUrl) {
        String url = jobUrl + JenkinsApiUtils.API_JSON;
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilderFactory, httpClient);
        return JenkinsApiUtils.getBody(response, WorkflowJob.class, objectMapper);
    }

    public Folder getFolderJobs(String folderUrl) {
        String url = folderUrl + JenkinsApiUtils.API_JSON;
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilderFactory, httpClient);
        return JenkinsApiUtils.getBody(response, Folder.class, objectMapper);
    }

    public QueueItem getQueueItem(String url) {
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url + JenkinsApiUtils.API_JSON, httpRequestBuilderFactory, httpClient);
        return JenkinsApiUtils.getBody(response, QueueItem.class, objectMapper);
    }

    public String getConsoleText(String jobUrl, int buildNumber) {
        String url = jobUrl + "/" + buildNumber + "/consoleText";
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilderFactory, httpClient);
        return response.body();
    }

    public ProgressiveConsoleText getProgressiveConsoleText(String jobUrl, int buildNumber, Long start) {
        return JenkinsApiUtils.getProgressiveConsoleText(jobUrl, buildNumber, start, httpRequestBuilderFactory, httpClient);
    }
}
