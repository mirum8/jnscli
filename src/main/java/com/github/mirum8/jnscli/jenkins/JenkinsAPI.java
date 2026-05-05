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
    private static final String JOBS_QUERY = "?tree=jobs[name,url,_class,color]";
    private static final String BUILD_INFO_QUERY = "?tree=number,displayName,timestamp,duration,result,"
        + "actions[causes[userId,userName],parameters[name,value]]";

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
            JenkinsApiUtils.sendRequest(HttpMethod.GET, JenkinsApiUtils.joinPath(settings.server(), JenkinsApiUtils.API_JSON), httpRequestBuilderFactory, httpClient);
            return new CheckConnectionResult(CheckConnectionResult.Status.SUCCESS, "Connection to Jenkins server " + settings.server() + " was successful");
        } catch (Exception e) {
            return new CheckConnectionResult(CheckConnectionResult.Status.FAILURE, e.getMessage());
        }
    }

    public BuildInfo getJobBuildInfo(String jobUrl, long buildNumber) {
        String url = JenkinsApiUtils.joinPath(jobUrl, buildNumber + JenkinsApiUtils.API_JSON) + BUILD_INFO_QUERY;
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilderFactory, httpClient);
        return JenkinsApiUtils.getBody(response, BuildInfo.class, objectMapper);
    }

    public List<Job> getJobs() {
        Settings settings = settingsService.readSettings();
        String url = JenkinsApiUtils.joinPath(settings.server(), "view/all/api/json") + JOBS_QUERY;
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilderFactory, httpClient);
        ViewAll viewAll = JenkinsApiUtils.getBody(response, ViewAll.class, objectMapper);
        return viewAll != null && viewAll.jobs() != null ? viewAll.jobs() : List.of();
    }

    public QueueItemLocation runJob(String jobUrl) {
        String url = JenkinsApiUtils.joinPath(jobUrl, "build");
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

    public void abortJob(String jobUrl, long buildNumber) {
        String url = JenkinsApiUtils.joinPath(jobUrl, buildNumber + "/stop");
        JenkinsApiUtils.sendRequest(HttpMethod.POST, url, httpRequestBuilderFactory, httpClient);
    }

    public WorkflowJob getWorkflowJob(String jobUrl) {
        String url = JenkinsApiUtils.joinPath(jobUrl, JenkinsApiUtils.API_JSON);
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilderFactory, httpClient);
        return JenkinsApiUtils.getBody(response, WorkflowJob.class, objectMapper);
    }

    public Folder getFolderJobs(String folderUrl) {
        String url = JenkinsApiUtils.joinPath(folderUrl, JenkinsApiUtils.API_JSON);
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilderFactory, httpClient);
        return JenkinsApiUtils.getBody(response, Folder.class, objectMapper);
    }

    public QueueItem getQueueItem(String url) {
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, JenkinsApiUtils.joinPath(url, JenkinsApiUtils.API_JSON), httpRequestBuilderFactory, httpClient);
        return JenkinsApiUtils.getBody(response, QueueItem.class, objectMapper);
    }

    public String getConsoleText(String jobUrl, long buildNumber) {
        String url = JenkinsApiUtils.joinPath(jobUrl, buildNumber + "/consoleText");
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilderFactory, httpClient);
        return response.body();
    }

    public ProgressiveConsoleText getProgressiveConsoleText(String jobUrl, long buildNumber, Long start) {
        return JenkinsApiUtils.getProgressiveConsoleText(jobUrl, buildNumber, start, httpRequestBuilderFactory, httpClient);
    }
}
