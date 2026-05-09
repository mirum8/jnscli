package com.github.mirum8.jnscli.mcp;

import com.github.mirum8.jnscli.abort.AbortService;
import com.github.mirum8.jnscli.ai.AiService;
import com.github.mirum8.jnscli.alias.AliasService;
import com.github.mirum8.jnscli.common.JobDescriptorProvider;
import com.github.mirum8.jnscli.context.JobType;
import com.github.mirum8.jnscli.creds.CredsFileWriter;
import com.github.mirum8.jnscli.creds.PasswordGenerator;
import com.github.mirum8.jnscli.diagnose.ErrorService;
import com.github.mirum8.jnscli.info.InfoService;
import com.github.mirum8.jnscli.jenkins.CredentialsAPI;
import com.github.mirum8.jnscli.jenkins.JenkinsAPI;
import com.github.mirum8.jnscli.jenkins.QueueItemLocation;
import com.github.mirum8.jnscli.jenkins.WorkflowJob;
import com.github.mirum8.jnscli.list.ListService;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.pipeline.PipelineCreateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class McpToolsTest {

    private final ListService listService = mock(ListService.class);
    private final InfoService infoService = mock(InfoService.class);
    private final ErrorService errorService = mock(ErrorService.class);
    private final AbortService abortService = mock(AbortService.class);
    private final AiService aiService = mock(AiService.class);
    private final JenkinsAPI jenkinsAPI = mock(JenkinsAPI.class);
    private final JobDescriptorProvider jobDescriptorProvider = mock(JobDescriptorProvider.class);
    private final AliasService aliasService = mock(AliasService.class);
    private final McpJsonCapture capture = new McpJsonCapture();
    private final CredentialsAPI credentialsAPI = mock(CredentialsAPI.class);
    private final PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
    private final CredsFileWriter credsFileWriter = mock(CredsFileWriter.class);
    private final PipelineCreateService pipelineCreateService = mock(PipelineCreateService.class);

    private McpTools toolsUnrestricted;
    private McpTools toolsRestricted;

    @BeforeEach
    void setUp() {
        toolsUnrestricted = new McpTools(listService, infoService, errorService, abortService, aiService,
            jenkinsAPI, jobDescriptorProvider, new AllowedJobs(null, aliasService), capture,
            credentialsAPI, passwordGenerator, credsFileWriter, pipelineCreateService);
        toolsRestricted = new McpTools(listService, infoService, errorService, abortService, aiService,
            jenkinsAPI, jobDescriptorProvider, new AllowedJobs("job-a", aliasService), capture,
            credentialsAPI, passwordGenerator, credsFileWriter, pipelineCreateService);
    }

    @Test
    void listJobsUnrestrictedReturnsCapturedOutputUnchanged() {
        doAnswer(inv -> {
            capture.set("[{\"id\":1,\"name\":\"job-a\"}]");
            return null;
        }).when(listService).listJobs();

        assertThat(toolsUnrestricted.listJobs(null)).isEqualTo("[{\"id\":1,\"name\":\"job-a\"}]");
    }

    @Test
    void listJobsRestrictedFiltersToAllowlist() {
        doAnswer(inv -> {
            capture.set("[{\"id\":1,\"name\":\"job-a\",\"url\":\"u-a\",\"type\":\"WORKFLOW\",\"color\":\"blue\"},"
                + "{\"id\":2,\"name\":\"job-b\",\"url\":\"u-b\",\"type\":\"WORKFLOW\",\"color\":\"red\"}]");
            return null;
        }).when(listService).listJobs();

        String result = toolsRestricted.listJobs(null);
        assertThat(result)
            .contains("job-a")
            .doesNotContain("job-b");
    }

    @Test
    void listJobsAcceptsFolderArg() {
        doAnswer(inv -> {
            capture.set("[]");
            return null;
        }).when(listService).listJobs("my-folder");

        assertThat(toolsUnrestricted.listJobs("my-folder")).isEqualTo("[]");
        verify(listService).listJobs("my-folder");
    }

    @Test
    void getJobInfoDeniesJobOutsideAllowlist() {
        assertThatThrownBy(() -> toolsRestricted.getJobInfo("job-b", null, null))
            .isInstanceOf(McpToolDeniedException.class);
        verify(infoService, never()).info(any(), any(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyBoolean());
    }

    @Test
    void getJobInfoUsesDefaultLimitWhenNotProvided() {
        doAnswer(inv -> {
            capture.set("{}");
            return null;
        }).when(infoService).info(eq("job-a"), any(), anyBoolean(), anyBoolean(), anyBoolean(), eq(10), anyBoolean());

        toolsRestricted.getJobInfo("job-a", null, null);
        verify(infoService).info("job-a", null, false, false, false, 10, false);
    }

    @Test
    void triggerBuildDeniesJobOutsideAllowlist() {
        assertThatThrownBy(() -> toolsRestricted.triggerBuild("job-b", null))
            .isInstanceOf(McpToolDeniedException.class);
        verify(jenkinsAPI, never()).runJob(any());
    }

    @Test
    void triggerBuildWithoutParametersCallsRunJob() {
        JobDescriptor descriptor = JobDescriptor.builder().name("job-a").url("https://j/job/job-a/").type(JobType.WORKFLOW).build();
        when(jobDescriptorProvider.get("job-a")).thenReturn(Optional.of(descriptor));
        when(jenkinsAPI.getWorkflowJob("https://j/job/job-a/")).thenReturn(buildableWorkflowJob(42));
        when(jenkinsAPI.runJob("https://j/job/job-a/")).thenReturn(new QueueItemLocation("https://j/queue/77/"));

        McpTools.TriggerBuildResult result = toolsRestricted.triggerBuild("job-a", null);
        assertThat(result.job()).isEqualTo("job-a");
        assertThat(result.buildNumber()).isEqualTo(42);
        assertThat(result.queueLocation()).isEqualTo("https://j/queue/77/");
        verify(jenkinsAPI, times(1)).runJob("https://j/job/job-a/");
        verify(jenkinsAPI, never()).runJob(any(), any());
    }

    @Test
    void triggerBuildWithParametersCallsRunJobWithList() {
        JobDescriptor descriptor = JobDescriptor.builder().name("job-a").url("https://j/job/job-a/").type(JobType.WORKFLOW).build();
        when(jobDescriptorProvider.get("job-a")).thenReturn(Optional.of(descriptor));
        when(jenkinsAPI.getWorkflowJob("https://j/job/job-a/")).thenReturn(buildableWorkflowJob(7));
        when(jenkinsAPI.runJob(eq("https://j/job/job-a/"), any())).thenReturn(new QueueItemLocation("https://j/queue/1/"));

        toolsRestricted.triggerBuild("job-a", Map.of("BRANCH", "main", "VERSION", "1.0"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<String>> paramsCaptor = ArgumentCaptor.forClass(List.class);
        verify(jenkinsAPI).runJob(eq("https://j/job/job-a/"), paramsCaptor.capture());
        assertThat(paramsCaptor.getValue()).containsExactlyInAnyOrder("BRANCH=main", "VERSION=1.0");
    }

    @Test
    void triggerBuildRejectsNonBuildableJob() {
        JobDescriptor descriptor = JobDescriptor.builder().name("job-a").url("https://j/job/job-a/").type(JobType.WORKFLOW).build();
        when(jobDescriptorProvider.get("job-a")).thenReturn(Optional.of(descriptor));
        WorkflowJob unbuildable = new WorkflowJob("job-a", "job-a", "https://j/job/job-a/", false,
            "blue", List.of(), null, List.of(), List.of(), 1, null);
        when(jenkinsAPI.getWorkflowJob("https://j/job/job-a/")).thenReturn(unbuildable);

        assertThatThrownBy(() -> toolsRestricted.triggerBuild("job-a", null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("not buildable");
    }

    @Test
    void abortBuildDeniesJobOutsideAllowlist() {
        assertThatThrownBy(() -> toolsRestricted.abortBuild("job-b", null))
            .isInstanceOf(McpToolDeniedException.class);
        verify(abortService, never()).abort(any());
    }

    @Test
    void abortBuildWithoutNumberDelegatesToService() {
        doAnswer(inv -> {
            capture.set("{\"job\":\"job-a\",\"buildNumber\":0,\"status\":\"ABORT_REQUESTED\"}");
            return null;
        }).when(abortService).abort("job-a");

        assertThat(toolsRestricted.abortBuild("job-a", null)).contains("ABORT_REQUESTED");
        verify(abortService).abort("job-a");
    }

    @Test
    void abortBuildWithNumberDelegatesToService() {
        doAnswer(inv -> {
            capture.set("{\"job\":\"job-a\",\"buildNumber\":12,\"status\":\"ABORT_REQUESTED\"}");
            return null;
        }).when(abortService).abort("job-a", 12);

        assertThat(toolsRestricted.abortBuild("job-a", 12)).contains("12");
        verify(abortService).abort("job-a", 12);
    }

    @Test
    void getBuildErrorsDeniesJobOutsideAllowlist() {
        assertThatThrownBy(() -> toolsRestricted.getBuildErrors("job-b", null))
            .isInstanceOf(McpToolDeniedException.class);
        verify(errorService, never()).getError(any(), any(), anyBoolean(), anyBoolean());
    }

    @Test
    void getBuildErrorsAllowedDelegatesToService() {
        doAnswer(inv -> {
            capture.set("{\"buildNumber\":7,\"errors\":\"oops\"}");
            return null;
        }).when(errorService).getError("job-a", 7, false, false);

        assertThat(toolsRestricted.getBuildErrors("job-a", 7)).contains("oops");
    }

    @Test
    void analyzeBuildWithAiShortCircuitsWhenNoErrors() {
        JobDescriptor descriptor = JobDescriptor.builder().name("job-a").url("https://j/job/job-a/").type(JobType.WORKFLOW).build();
        when(jobDescriptorProvider.get("job-a")).thenReturn(Optional.of(descriptor));
        when(errorService.getErrors(descriptor, 5)).thenReturn("");

        assertThat(toolsRestricted.analyzeBuildWithAi("job-a", 5)).isEqualTo("No errors found for build 5");
        verify(aiService, never()).analyzeLog(any());
    }

    @Test
    void analyzeBuildWithAiCallsAiServiceWhenErrorsPresent() {
        JobDescriptor descriptor = JobDescriptor.builder().name("job-a").url("https://j/job/job-a/").type(JobType.WORKFLOW).build();
        when(jobDescriptorProvider.get("job-a")).thenReturn(Optional.of(descriptor));
        when(errorService.getErrors(descriptor, 5)).thenReturn("BOOM");
        when(aiService.analyzeLog("BOOM")).thenReturn("It broke");

        assertThat(toolsRestricted.analyzeBuildWithAi("job-a", 5)).isEqualTo("It broke");
    }

    @Test
    void analyzeBuildWithAiHandlesJobWithNoBuilds() {
        JobDescriptor descriptor = JobDescriptor.builder().name("job-a").url("https://j/job/job-a/").type(JobType.WORKFLOW).build();
        when(jobDescriptorProvider.get("job-a")).thenReturn(Optional.of(descriptor));
        WorkflowJob neverBuilt = new WorkflowJob("job-a", "job-a", "https://j/job/job-a/", true,
            "notbuilt", List.of(), null, List.of(), List.of(), 1, null);
        when(jenkinsAPI.getWorkflowJob("https://j/job/job-a/")).thenReturn(neverBuilt);

        assertThat(toolsRestricted.analyzeBuildWithAi("job-a", null))
            .isEqualTo("No builds found for job-a");
        verify(errorService, never()).getErrors(any(), anyInt());
    }

    @Test
    void createPipelineDeniesJobOutsideAllowlist() {
        assertThatThrownBy(() -> toolsRestricted.createPipeline("job-b", "https://example.com/r.git", null, null, null, null, null))
            .isInstanceOf(McpToolDeniedException.class);
        verify(pipelineCreateService, never()).createForMcp(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void createPipelineMapsServiceResultToToolResult() {
        when(pipelineCreateService.createForMcp("job-a", "https://example.com/r.git", "develop", "ci/Jenkinsfile", "team", "creds", "demo"))
            .thenReturn(new PipelineCreateService.CreatePipelineJson("job-a",
                "https://j/job/team/job/job-a", "https://example.com/r.git", "develop", "ci/Jenkinsfile", "team"));

        McpTools.CreatePipelineResult result = toolsRestricted.createPipeline(
            "job-a", "https://example.com/r.git", "develop", "ci/Jenkinsfile", "team", "creds", "demo");

        assertThat(result.name()).isEqualTo("job-a");
        assertThat(result.url()).isEqualTo("https://j/job/team/job/job-a");
        assertThat(result.repo()).isEqualTo("https://example.com/r.git");
        assertThat(result.branch()).isEqualTo("develop");
        assertThat(result.scriptPath()).isEqualTo("ci/Jenkinsfile");
        assertThat(result.folder()).isEqualTo("team");
    }

    private WorkflowJob buildableWorkflowJob(int nextBuildNumber) {
        return new WorkflowJob("job-a", "job-a", "https://j/job/job-a/", true,
            "blue", List.of(), null, List.of(), List.of(), nextBuildNumber, null);
    }
}
