package com.github.mirum8.jnscli.build;

import com.github.mirum8.jnscli.jenkins.PipelineAPI;
import com.github.mirum8.jnscli.jenkins.Status;
import com.github.mirum8.jnscli.jenkins.WorkflowRun;
import com.github.mirum8.jnscli.shell.Symbols;
import com.github.mirum8.jnscli.shell.TestCapabilities;
import com.github.mirum8.jnscli.shell.Theme;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BuildProgressBarTest {

    @Test
    void completedReusesWorkflowRunFetchedDuringRunning() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        WorkflowRun firstRun = workflowRunWithStages(stage("compile", "IN_PROGRESS"));
        when(pipelineAPI.getJobBuildDescription("job-url", 1)).thenReturn(firstRun);
        BuildProgressBar bar = buildProgressBar(pipelineAPI, "job-url", 1);
        bar.running();

        List<String> actual = bar.completed();

        assertThat(actual).hasSize(1);
        assertThat(actual.getFirst()).contains("1 stage completed");
        verify(pipelineAPI, times(1)).getJobBuildDescription("job-url", 1);
    }

    @Test
    void failedRollsUpDoneStagesAndShowsFailingStage() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        WorkflowRun firstRun = workflowRunWithStages(
            stage("compile", "SUCCESS"),
            stage("test", "FAILED")
        );
        when(pipelineAPI.getJobBuildDescription("job-url", 1)).thenReturn(firstRun);
        BuildProgressBar bar = buildProgressBar(pipelineAPI, "job-url", 1);
        bar.running();

        List<String> actual = bar.failed();

        assertThat(actual).hasSize(2);
        assertThat(actual.get(0)).contains("1 stage completed");
        assertThat(actual.get(1)).contains("test");
        verify(pipelineAPI, times(1)).getJobBuildDescription("job-url", 1);
    }

    @Test
    void completedFetchesOnceWhenNoRunningTickHasFired() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        WorkflowRun run = workflowRunWithStages(stage("deploy", "SUCCESS"));
        when(pipelineAPI.getJobBuildDescription("job-url", 7)).thenReturn(run);
        BuildProgressBar bar = buildProgressBar(pipelineAPI, "job-url", 7);

        List<String> actual = bar.completed();

        assertThat(actual).hasSize(1);
        assertThat(actual.getFirst()).contains("1 stage completed");
        verify(pipelineAPI, times(1)).getJobBuildDescription("job-url", 7);
    }

    @Test
    void runningCollapsesDoneStagesAndListsPending() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        WorkflowRun previous = workflowRunWithStages(
            stage("compile", "SUCCESS"),
            stage("test", "SUCCESS"),
            stage("deploy", "SUCCESS")
        );
        WorkflowRun current = workflowRunWithStages(
            stage("compile", "SUCCESS"),
            stage("test", "IN_PROGRESS")
        );
        when(pipelineAPI.getJobBuildDescription("job-url", 1)).thenReturn(previous);
        when(pipelineAPI.getJobBuildDescription("job-url", 2)).thenReturn(current);
        BuildProgressBar bar = buildProgressBar(pipelineAPI, "job-url", 2);

        List<String> actual = bar.running();

        assertThat(actual).hasSize(3);
        assertThat(actual.get(0)).contains("1 stage completed");
        assertThat(actual.get(1)).contains("test");
        assertThat(actual.get(2)).contains("deploy").contains("(pending)");
    }

    @Test
    void runningOnFirstBuildHasNoPendingLine() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        WorkflowRun current = workflowRunWithStages(
            stage("compile", "SUCCESS"),
            stage("test", "IN_PROGRESS")
        );
        when(pipelineAPI.getJobBuildDescription("job-url", 1)).thenReturn(current);
        BuildProgressBar bar = buildProgressBar(pipelineAPI, "job-url", 1);

        List<String> actual = bar.running();

        assertThat(actual).hasSize(2);
        assertThat(actual.get(0)).contains("1 stage completed");
        assertThat(actual.get(1)).contains("test");
    }

    @Test
    void failedShowsSkippedSuffixForUnstartedStages() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        WorkflowRun previous = workflowRunWithStages(
            stage("compile", "SUCCESS"),
            stage("test", "SUCCESS"),
            stage("deploy", "SUCCESS"),
            stage("publish", "SUCCESS")
        );
        WorkflowRun current = workflowRunWithStages(
            stage("compile", "SUCCESS"),
            stage("test", "FAILED")
        );
        when(pipelineAPI.getJobBuildDescription("job-url", 1)).thenReturn(previous);
        when(pipelineAPI.getJobBuildDescription("job-url", 2)).thenReturn(current);
        BuildProgressBar bar = buildProgressBar(pipelineAPI, "job-url", 2);
        bar.running();

        List<String> actual = bar.failed();

        assertThat(actual).hasSize(3);
        assertThat(actual.get(0)).contains("1 stage completed");
        assertThat(actual.get(1)).contains("test");
        assertThat(actual.get(2)).contains("deploy").contains("publish").contains("(skipped)");
    }

    @Test
    void runningStageLineIncludesSpinnerFrame() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        WorkflowRun current = workflowRunWithStages(stage("compile", "IN_PROGRESS"));
        when(pipelineAPI.getJobBuildDescription("job-url", 1)).thenReturn(current);
        BuildProgressBar bar = unicodeBuildProgressBar(pipelineAPI, "job-url", 1);

        List<String> actual = bar.running();

        assertThat(actual).hasSize(1);
        assertThat(actual.getFirst().codePoints()).anyMatch(c -> "⠋⠙⠹⠸⠼⠴⠦⠧⠇⠏".indexOf(c) >= 0);
    }

    @Test
    void spinnerFrameAdvancesBetweenTicks() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        WorkflowRun current = workflowRunWithStages(stage("compile", "IN_PROGRESS"));
        when(pipelineAPI.getJobBuildDescription("job-url", 1)).thenReturn(current);
        BuildProgressBar bar = unicodeBuildProgressBar(pipelineAPI, "job-url", 1);

        String firstFrame = extractFirstChar(bar.running().getFirst());
        String secondFrame = extractFirstChar(bar.running().getFirst());

        assertThat(firstFrame).isNotEqualTo(secondFrame);
    }

    @Test
    void runningTicksWithinFetchIntervalDoNotRefetchFromJenkins() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        WorkflowRun current = workflowRunWithStages(stage("compile", "IN_PROGRESS"));
        when(pipelineAPI.getJobBuildDescription("job-url", 1)).thenReturn(current);
        AtomicLong now = new AtomicLong(1_000_000L);
        BuildProgressBar bar = clockedBuildProgressBar(pipelineAPI, "job-url", 1, now::get);

        bar.running();
        now.addAndGet(100);
        bar.running();
        now.addAndGet(100);
        bar.running();

        verify(pipelineAPI, times(1)).getJobBuildDescription("job-url", 1);
    }

    @Test
    void runningRefetchesAfterFetchIntervalElapses() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        WorkflowRun current = workflowRunWithStages(stage("compile", "IN_PROGRESS"));
        when(pipelineAPI.getJobBuildDescription("job-url", 1)).thenReturn(current);
        AtomicLong now = new AtomicLong(1_000_000L);
        BuildProgressBar bar = clockedBuildProgressBar(pipelineAPI, "job-url", 1, now::get);

        bar.running();
        now.addAndGet(5_000L);
        bar.running();

        verify(pipelineAPI, times(2)).getJobBuildDescription("job-url", 1);
    }

    @Test
    void refreshIntervalIsFastEnoughForSpinnerAnimation() {
        PipelineAPI pipelineAPI = mock(PipelineAPI.class);
        BuildProgressBar bar = unicodeBuildProgressBar(pipelineAPI, "job-url", 1);

        assertThat(bar.refreshIntervalMillis()).isLessThanOrEqualTo(200);
    }

    @Test
    void formatDurationShortRange() {
        assertThat(BuildProgressBar.formatDuration(0L)).isEqualTo("0s");
        assertThat(BuildProgressBar.formatDuration(47_000L)).isEqualTo("47s");
        assertThat(BuildProgressBar.formatDuration(83_000L)).isEqualTo("1m 23s");
        assertThat(BuildProgressBar.formatDuration(3_660_000L)).isEqualTo("1h 01m");
        assertThat(BuildProgressBar.formatDuration(-50L)).isEqualTo("0s");
    }

    @Test
    void truncateJoinedDropsTrailingNamesUntilFits() {
        List<String> names = List.of("alpha", "beta", "gamma", "delta");
        assertThat(BuildProgressBar.truncateJoined(names, 100)).isEqualTo("alpha, beta, gamma, delta");
        assertThat(BuildProgressBar.truncateJoined(names, 14)).isEqualTo("alpha, beta, …");
        assertThat(BuildProgressBar.truncateJoined(names, 4)).isEqualTo("alp…");
        assertThat(BuildProgressBar.truncateJoined(List.of(), 50)).isEmpty();
    }

    private static BuildProgressBar buildProgressBar(PipelineAPI pipelineAPI, String url, int n) {
        var caps = TestCapabilities.disabled();
        var theme = new Theme(caps);
        var symbols = new Symbols(caps);
        return new BuildProgressBar(pipelineAPI, new PercentageBar(caps, theme, symbols), caps, theme, symbols, url, n);
    }

    private static BuildProgressBar unicodeBuildProgressBar(PipelineAPI pipelineAPI, String url, int n) {
        var caps = TestCapabilities.unicode(true);
        var theme = new Theme(caps);
        var symbols = new Symbols(caps);
        return new BuildProgressBar(pipelineAPI, new PercentageBar(caps, theme, symbols), caps, theme, symbols, url, n);
    }

    private static BuildProgressBar clockedBuildProgressBar(PipelineAPI pipelineAPI, String url, int n,
                                                            LongSupplier clock) {
        var caps = TestCapabilities.unicode(true);
        var theme = new Theme(caps);
        var symbols = new Symbols(caps);
        return new BuildProgressBar(pipelineAPI, new PercentageBar(caps, theme, symbols), caps, theme, symbols, url, n) {
            @Override
            long currentTimeMillis() {
                return clock.getAsLong();
            }
        };
    }

    private static String extractFirstChar(String line) {
        String stripped = line.replaceAll("\\[[;\\d]*m", "");
        return String.valueOf(stripped.charAt(0));
    }

    private static WorkflowRun.Stage stage(String name, String status) {
        return new WorkflowRun.Stage(name, name, status, 0L, 0L, 0L, List.of());
    }

    private static WorkflowRun workflowRunWithStages(WorkflowRun.Stage... stages) {
        return new WorkflowRun(1, "run", Status.SUCCESS, 0L, 0L, 0L, 0L, 0L, List.of(stages));
    }
}
