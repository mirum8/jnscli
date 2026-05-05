package com.github.mirum8.jnscli.build;

import com.github.mirum8.jnscli.jenkins.JenkinsAPI;
import com.github.mirum8.jnscli.jenkins.NodeDescription;
import com.github.mirum8.jnscli.jenkins.PipelineAPI;
import com.github.mirum8.jnscli.jenkins.ProgressiveConsoleText;
import com.github.mirum8.jnscli.jenkins.StageDescription;
import com.github.mirum8.jnscli.jenkins.Status;
import com.github.mirum8.jnscli.jenkins.WorkflowRun;
import com.github.mirum8.jnscli.shell.TerminalCapabilities;
import com.github.mirum8.jnscli.shell.TestCapabilities;
import com.github.mirum8.jnscli.shell.Theme;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BuildFooterTest {

    @Test
    void renderHidesEverythingWhenNoActiveStageAndNoLog() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        JenkinsAPI jenkinsAPI = mock(JenkinsAPI.class);
        when(jenkinsAPI.getProgressiveConsoleText(anyString(), anyLong(), anyLong()))
            .thenReturn(new ProgressiveConsoleText("", false, 0L));
        BuildFooter footer = footer(pipelineAPI, jenkinsAPI, () -> 1L);

        footer.refresh(workflowRun(stage("compile", "SUCCESS")));

        assertThat(footer.render()).isEmpty();
    }

    @Test
    void renderShowsStageOnlyWhenStepInfoUnavailable() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        when(pipelineAPI.getStageDescription("job-url", 1L, "stage-1")).thenReturn(null);
        JenkinsAPI jenkinsAPI = quietJenkins();
        BuildFooter footer = footer(pipelineAPI, jenkinsAPI, () -> 1L);

        footer.refresh(workflowRun(stageWithId("stage-1", "Build", "IN_PROGRESS")));

        List<String> rendered = footer.render();
        assertThat(rendered).hasSize(2);
        assertThat(rendered.get(0)).isEmpty();
        assertThat(rendered.get(1)).contains("Build");
    }

    @Test
    void renderShowsStageStepAndCommandWhenAvailable() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        StageDescription stageDescription = new StageDescription("stage-1", "IN_PROGRESS",
            List.of(new StageDescription.FlowNode("node-7", "IN_PROGRESS")));
        when(pipelineAPI.getStageDescription("job-url", 1L, "stage-1")).thenReturn(stageDescription);
        when(pipelineAPI.getNodeDescription("job-url", 1L, "node-7"))
            .thenReturn(new NodeDescription("node-7", "Shell Script", "IN_PROGRESS", "mvn clean package"));
        BuildFooter footer = footer(pipelineAPI, quietJenkins(), () -> 1L);

        footer.refresh(workflowRun(stageWithId("stage-1", "Build", "IN_PROGRESS")));

        List<String> rendered = footer.render();
        assertThat(rendered).hasSize(2);
        assertThat(rendered.get(1))
            .contains("Build")
            .contains("Shell Script")
            .contains("mvn clean package");
    }

    @Test
    void logLinesAppearBelowHeader() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        JenkinsAPI jenkinsAPI = mock(JenkinsAPI.class);
        when(jenkinsAPI.getProgressiveConsoleText(anyString(), anyLong(), anyLong()))
            .thenReturn(new ProgressiveConsoleText("[INFO] one\n[INFO] two\n", false, 100L));
        BuildFooter footer = footer(pipelineAPI, jenkinsAPI, () -> 1L);

        footer.refresh(workflowRun(stageWithId("stage-1", "Build", "IN_PROGRESS")));

        List<String> rendered = footer.render();
        assertThat(rendered).hasSize(4);
        assertThat(rendered.get(2)).contains("[INFO] one");
        assertThat(rendered.get(3)).contains("[INFO] two");
    }

    @Test
    void logBufferDropsOldestWhenFullCapacityExceeded() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        JenkinsAPI jenkinsAPI = mock(JenkinsAPI.class);
        when(jenkinsAPI.getProgressiveConsoleText(anyString(), anyLong(), anyLong()))
            .thenReturn(new ProgressiveConsoleText("a\nb\nc\nd\ne\nf\ng\n", false, 14L));
        BuildFooter footer = footer(pipelineAPI, jenkinsAPI, () -> 1L);

        footer.refresh(workflowRun(stageWithId("stage-1", "Build", "IN_PROGRESS")));

        List<String> rendered = footer.render();
        assertThat(rendered).hasSize(7);
        assertThat(rendered.get(2)).contains("c");
        assertThat(rendered.get(6)).contains("g");
        assertThat(String.join(" ", rendered)).doesNotContain(" a ").doesNotContain(" b ");
    }

    @Test
    void ansiSequencesAreStrippedFromLog() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        JenkinsAPI jenkinsAPI = mock(JenkinsAPI.class);
        when(jenkinsAPI.getProgressiveConsoleText(anyString(), anyLong(), anyLong()))
            .thenReturn(new ProgressiveConsoleText("[31mred[0m line\n", false, 10L));
        BuildFooter footer = footer(pipelineAPI, jenkinsAPI, () -> 1L);

        footer.refresh(workflowRun(stageWithId("stage-1", "Build", "IN_PROGRESS")));

        String logLine = footer.render().get(2);
        assertThat(logLine).contains("red line").doesNotContain("[31m");
    }

    @Test
    void emptyLinesAreSkipped() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        JenkinsAPI jenkinsAPI = mock(JenkinsAPI.class);
        when(jenkinsAPI.getProgressiveConsoleText(anyString(), anyLong(), anyLong()))
            .thenReturn(new ProgressiveConsoleText("real\n\n   \nalsoreal\n", false, 20L));
        BuildFooter footer = footer(pipelineAPI, jenkinsAPI, () -> 1L);

        footer.refresh(workflowRun(stageWithId("stage-1", "Build", "IN_PROGRESS")));

        List<String> rendered = footer.render();
        assertThat(rendered).hasSize(4);
        assertThat(rendered.get(2)).contains("real");
        assertThat(rendered.get(3)).contains("alsoreal");
    }

    @Test
    void logFetchIsThrottled() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        JenkinsAPI jenkinsAPI = quietJenkins();
        AtomicLong clock = new AtomicLong(1_000_000L);
        BuildFooter footer = footer(pipelineAPI, jenkinsAPI, clock::get);

        WorkflowRun run = workflowRun(stageWithId("stage-1", "Build", "IN_PROGRESS"));
        footer.refresh(run);
        clock.addAndGet(100L);
        footer.refresh(run);
        clock.addAndGet(2_500L);
        footer.refresh(run);

        verify(jenkinsAPI, times(1)).getProgressiveConsoleText(anyString(), anyLong(), anyLong());
    }

    @Test
    void logFetchHappensAgainAfterLogInterval() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        JenkinsAPI jenkinsAPI = quietJenkins();
        AtomicLong clock = new AtomicLong(1_000_000L);
        BuildFooter footer = footer(pipelineAPI, jenkinsAPI, clock::get);

        WorkflowRun run = workflowRun(stageWithId("stage-1", "Build", "IN_PROGRESS"));
        footer.refresh(run);
        clock.addAndGet(3_000L);
        footer.refresh(run);

        verify(jenkinsAPI, times(2)).getProgressiveConsoleText(anyString(), anyLong(), anyLong());
    }

    @Test
    void stepFetchStaysOnFiveSecondCadenceWhileLogFetchesFaster() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        JenkinsAPI jenkinsAPI = quietJenkins();
        AtomicLong clock = new AtomicLong(1_000_000L);
        BuildFooter footer = footer(pipelineAPI, jenkinsAPI, clock::get);

        WorkflowRun run = workflowRun(stageWithId("stage-1", "Build", "IN_PROGRESS"));
        footer.refresh(run);
        clock.addAndGet(3_000L);
        footer.refresh(run);
        clock.addAndGet(1_500L);
        footer.refresh(run);

        verify(pipelineAPI, times(1)).getStageDescription(anyString(), anyLong(), anyString());
        verify(jenkinsAPI, times(2)).getProgressiveConsoleText(anyString(), anyLong(), anyLong());
    }

    @Test
    void cursorAdvancesAcrossFetches() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        JenkinsAPI jenkinsAPI = mock(JenkinsAPI.class);
        when(jenkinsAPI.getProgressiveConsoleText("job-url", 1, 0L))
            .thenReturn(new ProgressiveConsoleText("first\n", true, 6L));
        when(jenkinsAPI.getProgressiveConsoleText("job-url", 1, 6L))
            .thenReturn(new ProgressiveConsoleText("second\n", false, 13L));
        AtomicLong clock = new AtomicLong(0L);
        BuildFooter footer = footer(pipelineAPI, jenkinsAPI, clock::get);

        footer.refresh(workflowRun(stage("compile", "SUCCESS")));
        clock.addAndGet(3_000L);
        footer.refresh(workflowRun(stage("compile", "SUCCESS")));

        verify(jenkinsAPI).getProgressiveConsoleText("job-url", 1, 0L);
        verify(jenkinsAPI).getProgressiveConsoleText("job-url", 1, 6L);
    }

    @Test
    void firstCallFetchesEvenWhenClockReturnsZero() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        JenkinsAPI jenkinsAPI = quietJenkins();
        BuildFooter footer = footer(pipelineAPI, jenkinsAPI, () -> 0L);

        footer.refresh(workflowRun(stage("compile", "SUCCESS")));

        verify(jenkinsAPI, times(1)).getProgressiveConsoleText(anyString(), anyLong(), anyLong());
    }

    @Test
    void mostRecentlyStartedInProgressStageWins() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        when(pipelineAPI.getStageDescription("job-url", 1L, "later")).thenReturn(null);
        BuildFooter footer = footer(pipelineAPI, quietJenkins(), () -> 1L);

        WorkflowRun run = new WorkflowRun(1, "run", Status.IN_PROGRESS, 0L, 0L, 0L, 0L, 0L,
            List.of(
                new WorkflowRun.Stage("earlier", "Earlier", "IN_PROGRESS", 1_000L, 0L, 0L, List.of()),
                new WorkflowRun.Stage("later", "Later", "IN_PROGRESS", 5_000L, 0L, 0L, List.of())
            ));
        footer.refresh(run);

        verify(pipelineAPI).getStageDescription("job-url", 1L, "later");
        verify(pipelineAPI, never()).getStageDescription("job-url", 1L, "earlier");
    }

    @Test
    void headerClearsWhenStageDisappearsOnNextStepRefresh() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        StageDescription stageDescription = new StageDescription("stage-1", "IN_PROGRESS",
            List.of(new StageDescription.FlowNode("node-7", "IN_PROGRESS")));
        when(pipelineAPI.getStageDescription("job-url", 1L, "stage-1")).thenReturn(stageDescription);
        when(pipelineAPI.getNodeDescription("job-url", 1L, "node-7"))
            .thenReturn(new NodeDescription("node-7", "Shell Script", "IN_PROGRESS", "mvn"));
        AtomicLong clock = new AtomicLong(1_000_000L);
        BuildFooter footer = footer(pipelineAPI, quietJenkins(), clock::get);

        footer.refresh(workflowRun(stageWithId("stage-1", "Build", "IN_PROGRESS")));
        clock.addAndGet(5_000L);
        footer.refresh(workflowRun(stage("compile", "SUCCESS")));

        assertThat(footer.render()).isEmpty();
    }

    @Test
    void tailTruncateKeepsTailWithEllipsis() {
        assertThat(BuildFooter.tailTruncate("short", 10)).isEqualTo("short");
        assertThat(BuildFooter.tailTruncate("0123456789abcdef", 10)).isEqualTo("…789abcdef");
        assertThat(BuildFooter.tailTruncate("anything", 0)).isEmpty();
    }

    private static BuildFooter footer(PipelineAPI pipelineAPI, JenkinsAPI jenkinsAPI, LongSupplier clock) {
        TerminalCapabilities caps = TestCapabilities.of(120, false, false);
        Theme theme = new Theme(caps);
        return new BuildFooter(pipelineAPI, jenkinsAPI, caps, theme, "job-url", 1, clock);
    }

    private static JenkinsAPI quietJenkins() {
        JenkinsAPI api = mock(JenkinsAPI.class);
        when(api.getProgressiveConsoleText(anyString(), anyLong(), anyLong()))
            .thenReturn(new ProgressiveConsoleText("", false, 0L));
        return api;
    }

    private static WorkflowRun.Stage stage(String name, String status) {
        return new WorkflowRun.Stage(name, name, status, 0L, 0L, 0L, List.of());
    }

    private static WorkflowRun.Stage stageWithId(String id, String name, String status) {
        return new WorkflowRun.Stage(id, name, status, 0L, 0L, 0L, List.of());
    }

    private static WorkflowRun workflowRun(WorkflowRun.Stage... stages) {
        return new WorkflowRun(1, "run", Status.IN_PROGRESS, 0L, 0L, 0L, 0L, 0L, List.of(stages));
    }
}
