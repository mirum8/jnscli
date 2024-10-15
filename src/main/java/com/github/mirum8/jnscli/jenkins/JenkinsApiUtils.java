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
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.logging.Logger;

public class JenkinsApiUtils {
    private static final Logger log = Logger.getLogger(JenkinsApiUtils.class.getName());

    private JenkinsApiUtils() {
    }

    public static final String API_JSON = "/api/json";
    private static final String BOUNDARY = "ZeeBoundX12345AbCdEf";
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long INITIAL_RETRY_DELAY_MS = 1000;

    public static ObjectMapper createObjectMapper() {
        return new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public static HttpResponse<String> sendRequest(HttpMethod httpMethod, String url, HttpRequestBuilder httpRequestBuilder, HttpClient httpClient) {
        int attempts = 0;
        while (attempts < MAX_RETRY_ATTEMPTS) {
            try {
                return send(httpMethod, url, httpRequestBuilder, httpClient);
            } catch (HttpTimeoutException e) {
                if (attempts < MAX_RETRY_ATTEMPTS - 1) {
                    attempts++;
                    long delay = calculateExponentialBackoff(attempts);
                    log.warning("Request failed. Retrying in " + delay + "ms...");
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new JenkinsAPIException("Retry interrupted", ie);
                    }
                } else {
                    throw new JenkinsAPIException("Max retry attempts reached", e);
                }
            }
        }
        throw new JenkinsAPIException("Max retry attempts reached");
    }

    private static HttpResponse<String> send(HttpMethod httpMethod, String url, HttpRequestBuilder httpRequestBuilder, HttpClient httpClient) throws HttpTimeoutException {
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
        } catch (HttpTimeoutException e) {
            throw e;
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
            httpRequestBuilder
                .url(url)
                .method(HttpMethod.POST)
                .header("Content-Type", "multipart/form-data; boundary=" + BOUNDARY)
                .body(buildMultipartBody(fileParamName, filePath));

            HttpResponse<String> response = sendRequest(HttpMethod.POST, url, httpRequestBuilder, httpClient);
            if (response.statusCode() != 200 && response.statusCode() != 201) {
                throw new JenkinsAPIException("Failed to run job with file parameter, status code: " + response.statusCode());
            }
            return getQueueItemLocation(response);
        } catch (IOException e) {
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

    private static long calculateExponentialBackoff(int attempt) {
        return JenkinsApiUtils.INITIAL_RETRY_DELAY_MS * (long) Math.pow(2, (attempt - 1));
    }
}
