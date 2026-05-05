package com.github.mirum8.jnscli.jenkins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mirum8.jnscli.http.HttpMethod;
import com.github.mirum8.jnscli.http.HttpRequestBuilderFactory;
import org.springframework.stereotype.Component;

import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.util.List;

@Component
public class PipelineAPI {
    private static final String WFAPI_DESCRIBE = "/wfapi/describe";
    private static final String EXECUTION_NODE = "/execution/node/";

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;
    private final HttpRequestBuilderFactory httpRequestBuilderFactory;

    public PipelineAPI(HttpClient httpClient,
                       HttpRequestBuilderFactory httpRequestBuilderFactory) {
        this.httpRequestBuilderFactory = httpRequestBuilderFactory;
        this.httpClient = httpClient;
        this.objectMapper = JenkinsApiUtils.createObjectMapper();
    }

    public WorkflowRun getJobBuildDescription(String jobUrl, long buildNumber) {
        String url = JenkinsApiUtils.joinPath(jobUrl, buildNumber + WFAPI_DESCRIBE);
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilderFactory, httpClient);
        return JenkinsApiUtils.getBody(response, WorkflowRun.class, objectMapper);
    }

    public List<Run> getJobRuns(String jobUrl) {
        String url = JenkinsApiUtils.joinPath(jobUrl, "wfapi/runs");
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilderFactory, httpClient);
        try {
            return objectMapper.readValue(response.body(), new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new JenkinsAPIException(e);
        }
    }

    public StageDescription getStageDescription(String jobUrl, long buildNumber, String stageId) {
        String url = JenkinsApiUtils.joinPath(jobUrl, buildNumber + EXECUTION_NODE + stageId + WFAPI_DESCRIBE);
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilderFactory, httpClient);
        return JenkinsApiUtils.getBody(response, StageDescription.class, objectMapper);
    }

    public NodeLog getNodeLog(String jobUrl, long buildNumber, String nodeId) {
        String url = JenkinsApiUtils.joinPath(jobUrl, buildNumber + EXECUTION_NODE + nodeId + "/wfapi/log");
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilderFactory, httpClient);
        return JenkinsApiUtils.getBody(response, NodeLog.class, objectMapper);
    }

    public NodeDescription getNodeDescription(String jobUrl, long buildNumber, String nodeId) {
        String url = JenkinsApiUtils.joinPath(jobUrl, buildNumber + EXECUTION_NODE + nodeId + WFAPI_DESCRIBE);
        HttpResponse<String> response = JenkinsApiUtils.sendRequest(HttpMethod.GET, url, httpRequestBuilderFactory, httpClient);
        return JenkinsApiUtils.getBody(response, NodeDescription.class, objectMapper);
    }
}
