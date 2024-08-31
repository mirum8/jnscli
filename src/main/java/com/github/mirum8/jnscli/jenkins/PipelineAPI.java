package com.github.mirum8.jnscli.jenkins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mirum8.jnscli.http.HttpMethod;
import com.github.mirum8.jnscli.http.HttpRequestBuilder;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;

@Component
public class PipelineAPI {
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final HttpRequestBuilder httpRequestBuilder;

    public PipelineAPI(HttpClient httpClient,
                       HttpRequestBuilder httpRequestBuilder) {
        this.httpRequestBuilder = httpRequestBuilder;
        this.httpClient = httpClient;
        this.objectMapper = JenkinsApiUtils.createObjectMapper();
    }

    public WorkflowRun getJobBuildDescription(String jobUrl, int buildNumber) {
        String url = jobUrl + "/" + buildNumber + "/wfapi/describe";
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilder, httpClient);
        return JenkinsApiUtils.getBody(response, WorkflowRun.class, objectMapper);
    }

    public List<Run> getJobRuns(String jobUrl) {
        String url = jobUrl.endsWith("/") ? jobUrl + "wfapi/runs" : jobUrl + "/wfapi/runs";
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilder, httpClient);
        try {
            return objectMapper.readValue(response.body(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new JenkinsAPIException(e);
        }
    }

    public WorkflowJob getWorkflowJob(String jobUrl) {
        String url = jobUrl + JenkinsApiUtils.API_JSON;
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilder, httpClient);
        return JenkinsApiUtils.getBody(response, WorkflowJob.class, objectMapper);
    }

    public StageDescription getStageDescription(String jobUrl, long buildNumber, String stageId) {
        String url = jobUrl + "/" + buildNumber + "/execution/node/" + stageId + "/wfapi/describe";
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilder, httpClient);
        return JenkinsApiUtils.getBody(response, StageDescription.class, objectMapper);
    }

    public NodeLog getNodeLog(String jobUrl, long buildNumber, String nodeId) {
        String url = jobUrl + "/" + buildNumber + "/execution/node/" + nodeId + "/wfapi/log";
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilder, httpClient);
        return JenkinsApiUtils.getBody(response, NodeLog.class, objectMapper);
    }
}
