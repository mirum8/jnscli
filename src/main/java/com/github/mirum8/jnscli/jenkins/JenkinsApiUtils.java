package com.github.mirum8.jnscli.jenkins;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mirum8.jnscli.http.HttpMethod;
import com.github.mirum8.jnscli.http.HttpRequestBuilder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class JenkinsApiUtils {
    private JenkinsApiUtils() {
    }

    public static final String API_JSON = "/api/json";
    private static final String BOUNDARY = "ZeeBoundX12345AbCdEf";

    public static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static HttpResponse<String> sendRequest(HttpMethod httpMethod, String url, HttpRequestBuilder httpRequestBuilder, HttpClient httpClient) {
        url = url.replace(" ", "%20");
        try {
            HttpRequest request = httpRequestBuilder
                .url(url)
                .method(httpMethod)
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new JenkinsAPIException("HTTP: " + response.statusCode() + "; URL: " + url);
            }
            return response;
        } catch (IOException e) {
            throw new JenkinsAPIException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JenkinsAPIException(e);
        }
    }

    public static <T> T getBody(HttpResponse<String> response, Class<T> clazz, ObjectMapper objectMapper) {
        try {
            return objectMapper.readValue(response.body(), clazz);
        } catch (JsonProcessingException e) {
            throw new JenkinsAPIException(e);
        }
    }

    public static QueueItemLocation getQueueItemLocation(HttpResponse<String> response) {
        String location = response.headers().firstValue("Location").orElse(null);
        if (location == null) {
            throw new JenkinsAPIException("No Location header in response");
        }
        return new QueueItemLocation(location);
    }

    public static QueueItemLocation runJobWithFileParam(String jobUrl, String fileParamName, Path filePath, List<String> parameters, HttpRequestBuilder httpRequestBuilder, HttpClient httpClient) {
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
                throw new JenkinsAPIException("Failed to run job with file parameter, status code: " + response.statusCode());
            }
            return getQueueItemLocation(response);
        } catch (IOException e) {
            throw new JenkinsAPIException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JenkinsAPIException(e);
        }
    }

    private static HttpRequest.BodyPublisher buildMultipartBody(String fileParamName, Path filePath) throws IOException {
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

    public static ProgressiveConsoleText getProgressiveConsoleText(String jobUrl, int buildNumber, Long start, HttpRequestBuilder httpRequestBuilder, HttpClient httpClient) {
        StringBuilder urlBuilder = new StringBuilder(jobUrl)
            .append("/").append(buildNumber)
            .append("/logText/progressiveText");

        if (start != null) {
            urlBuilder.append("?start=").append(start);
        }

        String url = urlBuilder.toString();
        HttpResponse<String> response = sendRequest(HttpMethod.GET, url, httpRequestBuilder, httpClient);

        boolean hasMoreData = Boolean.parseBoolean(response.headers().firstValue("X-More-Data").orElse("false"));
        long nextStart = Long.parseLong(response.headers().firstValue("X-Text-Size").orElse("0"));
        return new ProgressiveConsoleText(response.body(), hasMoreData, nextStart);
    }
}
