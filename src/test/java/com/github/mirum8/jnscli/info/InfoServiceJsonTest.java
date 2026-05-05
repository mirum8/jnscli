package com.github.mirum8.jnscli.info;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mirum8.jnscli.common.JobDescriptorProvider;
import com.github.mirum8.jnscli.context.JobType;
import com.github.mirum8.jnscli.jenkins.BuildInfo;
import com.github.mirum8.jnscli.jenkins.JenkinsAPI;
import com.github.mirum8.jnscli.jenkins.PipelineAPI;
import com.github.mirum8.jnscli.jenkins.Run;
import com.github.mirum8.jnscli.jenkins.Status;
import com.github.mirum8.jnscli.jenkins.WorkflowJob;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.settings.Settings;
import com.github.mirum8.jnscli.settings.SettingsService;
import com.github.mirum8.jnscli.shell.JsonOutput;
import com.github.mirum8.jnscli.shell.Messages;
import com.github.mirum8.jnscli.shell.OutputContext;
import com.github.mirum8.jnscli.shell.OutputMode;
import com.github.mirum8.jnscli.shell.Section;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.util.StatusFormatter;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InfoServiceJsonTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void jsonModeHandlesParameterWithNullValue() throws Exception {
        // given
        JenkinsAPI jenkinsAPI = mock(JenkinsAPI.class);
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        JobDescriptorProvider jobDescriptorProvider = mock(JobDescriptorProvider.class);
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.readSettings()).thenReturn(new Settings("https://j", "me", "k"));

        JobDescriptor descriptor = JobDescriptor.builder()
            .name("job-a")
            .url("https://j/job/job-a/")
            .type(JobType.WORKFLOW)
            .build();
        when(jobDescriptorProvider.get("job-a")).thenReturn(Optional.of(descriptor));

        WorkflowJob workflowJob = new WorkflowJob("job-a", "job-a", "https://j/job/job-a/", true,
            "blue", List.of(), null, List.of(), List.of(), 1, null);
        when(jenkinsAPI.getWorkflowJob("https://j/job/job-a/")).thenReturn(workflowJob);

        Run run = new Run(7, "#7", Status.SUCCESS, List.of());
        when(pipelineAPI.getJobRuns("https://j/job/job-a/")).thenReturn(List.of(run));

        BuildInfo.Action.Parameter paramNullValue = new BuildInfo.Action.Parameter("PASSWORD", null);
        BuildInfo.Action.Parameter paramOk = new BuildInfo.Action.Parameter("BRANCH", "main");
        BuildInfo.Action action = new BuildInfo.Action(List.of(paramNullValue, paramOk), List.of());
        BuildInfo buildInfo = new BuildInfo(7, "#7", 1234L, 56L, "desc",
            List.of(action), Status.SUCCESS);
        when(jenkinsAPI.getJobBuildInfo("https://j/job/job-a/", 7)).thenReturn(buildInfo);

        List<Object> captured = new ArrayList<>();
        JsonOutput jsonOutput = new JsonOutput(mock(ShellPrinter.class)) {
            @Override
            public void println(Object value) {
                captured.add(value);
            }
        };

        OutputContext outputContext = new OutputContext(OutputMode.JSON);
        InfoService infoService = new InfoService(
            jenkinsAPI,
            mock(ShellPrinter.class),
            mock(Messages.class),
            jobDescriptorProvider,
            settingsService,
            pipelineAPI,
            mock(CommandRunner.class),
            mock(Section.class),
            mock(StatusFormatter.class),
            outputContext,
            jsonOutput);

        // when
        infoService.info("job-a", null, false, false, false, 10, false);

        // then
        assertThat(captured).hasSize(1);
        String json = mapper.writeValueAsString(captured.get(0));
        JsonNode tree = mapper.readTree(json);
        JsonNode params = tree.get("builds").get(0).get("parameters");
        assertThat(params.get("BRANCH").asText()).isEqualTo("main");
        assertThat(params.has("PASSWORD")).isTrue();
    }
}
