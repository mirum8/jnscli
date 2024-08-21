package com.github.mirum8.jnscli.build.parameters.activechoises;

import com.github.mirum8.jnscli.build.parameters.DynamicReferencedParameterPrompter;
import com.github.mirum8.jnscli.http.HttpMethod;
import com.github.mirum8.jnscli.http.HttpRequestBuilder;
import com.github.mirum8.jnscli.jenkins.WorkflowJob;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class ActiveChoicesReactiveParameterPrompter implements DynamicReferencedParameterPrompter {

    private static final Pattern CRUMBS_PATTERN = Pattern.compile("makeStaplerProxy\\('/\\$stapler/bound/[\\w-]+','([\\w]+)'");

    private final ShellPrompter shellPrompter;
    private final HttpClient httpClient;
    private final HttpRequestBuilder httpRequestBuilder;
    private final ActiveChoiceExtractor activeChoiceExtractor;

    private String activeChoiceHtml;

    public ActiveChoicesReactiveParameterPrompter(ShellPrompter shellPrompter, HttpClient httpClient, HttpRequestBuilder httpRequestBuilder, ActiveChoiceExtractor activeChoiceExtractor) {
        this.shellPrompter = shellPrompter;
        this.httpClient = httpClient;
        this.httpRequestBuilder = httpRequestBuilder;
        this.activeChoiceExtractor = activeChoiceExtractor;
    }

    @Override
    public String prompt(WorkflowJob job, WorkflowJob.Property.ParameterDefinition parameterDefinition, Map<String, String> referenceParameters) {
        String html = getHtmlForParsingActiveChoices(job);
        ActiveChoice activeChoice = activeChoiceExtractor.getActiveChoice(parameterDefinition.name(), html);

        if (activeChoice == ActiveChoice.NOT_FOUND) {
            throw new IllegalStateException("Active choice not found for parameter " + parameterDefinition.name());
        }

        String crumb = activeChoice.legacy() ? activeChoice.crumbs() : initCascadeChoiceParameter(activeChoice);
        doUpdate(activeChoice, referenceParameters, crumb);
        List<String> choices = getChoices(activeChoice, crumb);

        return shellPrompter.promptSelectFromList(parameterDefinition.name(), choices);
    }

    String initCascadeChoiceParameter(ActiveChoice activeChoice) {
        try {
            String path = "/$stapler/bound/script/$stapler/bound/" + activeChoice.uid() + "?var=cascadeChoiceParameter&methods=doUpdate,getChoicesForUI";
            HttpResponse<String> response = httpClient.send(
                httpRequestBuilder.method(HttpMethod.GET)
                    .path(path)
                    .header("Accept", "*/*")
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                throw new IllegalStateException("Failed to initialize cascade choice parameter, status code: " + response.statusCode());
            }

            Matcher matcher = CRUMBS_PATTERN.matcher(response.body());
            if (matcher.find()) {
                return matcher.group(1);
            } else {
                throw new IllegalStateException("Failed to initialize cascade choice parameter, no crumb found");
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    List<String> getChoices(ActiveChoice activeChoice, String crumb) {
        try {
            HttpResponse<String> response = httpClient.send(
                httpRequestBuilder.method(HttpMethod.POST)
                    .path("/$stapler/bound/" + activeChoice.uid() + "/getChoicesForUI")
                    .header("Content-Type", "application/x-stapler-method-invocation;charset=UTF-8")
                    .header("Crumb", crumb)
                    .header("Jenkins-Crumb", crumb)
                    .body(HttpRequest.BodyPublishers.ofString("[]"))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() != 200) {
                throw new IllegalStateException("Failed to get options, status code: " + response.statusCode());
            }
            return Stream.of(response.body().split(","))
                .map(s -> s.replace("\"", "").replace("[", "").replace("]", ""))
                .distinct()
                .toList();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    void doUpdate(ActiveChoice activeChoice, Map<String, String> referenceParameters, String crumb) {

        String body = "[\"" +
            referenceParameters.entrySet().stream()
                .filter(entry -> activeChoice.referencedParameter().contains(entry.getKey()))
                .map(this::convertBooleanValue)
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("__LESEP__")) +
            "\"]";


        try {
            String path = "/$stapler/bound/" + activeChoice.uid() + "/doUpdate";
            HttpResponse<String> response = httpClient.send(
                httpRequestBuilder.method(HttpMethod.POST)
                    .header("Content-Type", "application/x-stapler-method-invocation;charset=UTF-8")
                    .header("Crumb", crumb)
                    .header("Jenkins-Crumb", crumb)
                    .path(path)
                    .body(HttpRequest.BodyPublishers.ofString(body))
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() != 204) {
                throw new IllegalStateException("Failed to invoke groovy script, status code: " + response.statusCode());
            }
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    private Map.Entry<String, String> convertBooleanValue(Map.Entry<String, String> stringStringEntry) {
        if (stringStringEntry.getValue().equals("true")) {
            return Map.entry(stringStringEntry.getKey(), "on");
        } else if (stringStringEntry.getValue().equals("false")) {
            return Map.entry(stringStringEntry.getKey(), "");
        } else {
            return stringStringEntry;
        }
    }

    String getHtmlForParsingActiveChoices(WorkflowJob job) {
        if (activeChoiceHtml != null) {
            return activeChoiceHtml;
        }
        try {
            HttpResponse<String> response = httpClient.send(
                httpRequestBuilder.method(HttpMethod.GET).url(job.url() + "build?delay=0sec").build(),
                HttpResponse.BodyHandlers.ofString()
            );
            if (response.statusCode() != 405) {
                throw new IllegalStateException("Failed to get html for parsing active choices, status code: " + response.statusCode());
            }
            activeChoiceHtml = response.body();
            return activeChoiceHtml;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Set<String> applicableForTypes() {
        return Set.of("CascadeChoiceParameter");
    }
}
