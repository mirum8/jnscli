package com.github.mirum8.jnscli.build;

import com.github.mirum8.jnscli.jenkins.PipelineAPI;
import com.github.mirum8.jnscli.jenkins.WorkflowRun;
import com.github.mirum8.jnscli.runner.ProgressBar;
import com.github.mirum8.jnscli.shell.Symbols;
import com.github.mirum8.jnscli.shell.TerminalCapabilities;
import com.github.mirum8.jnscli.shell.Theme;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BuildProgressBar implements ProgressBar {
    private static final long DEFAULT_STAGE_DURATION_MS = 60_000L;
    private static final int REFRESH_INTERVAL_MS = 100;
    private static final long FETCH_INTERVAL_MS = 5000L;
    private static final int MAX_RUNNING_PERCENT = 99;
    private static final int MIN_PENDING_BUDGET = 20;
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    private static final String STATUS_FAILED = "FAILED";
    private static final String PENDING_SUFFIX = " (pending)";
    private static final String SKIPPED_SUFFIX = " (skipped)";
    private static final String ELLIPSIS = "…";

    private final PipelineAPI pipelineAPI;
    private final PercentageBar percentageBar;
    private final TerminalCapabilities caps;
    private final Theme theme;
    private final Symbols symbols;
    private final String jobUrl;
    private final int buildNumber;
    private final char[] spinnerFrames;
    private List<WorkflowRun.Stage> previousBuildStages;
    private WorkflowRun latestRun;
    private long latestRunFetchedAtMillis;
    private int spinCounter;

    public BuildProgressBar(PipelineAPI pipelineAPI, PercentageBar percentageBar,
                            TerminalCapabilities caps, Theme theme, Symbols symbols,
                            String jobUrl, int buildNumber) {
        this.pipelineAPI = pipelineAPI;
        this.percentageBar = percentageBar;
        this.caps = caps;
        this.theme = theme;
        this.symbols = symbols;
        this.jobUrl = jobUrl;
        this.buildNumber = buildNumber;
        this.spinnerFrames = symbols.spinnerFrames();
    }

    @Override
    public List<String> running() {
        WorkflowRun run = currentRun();
        spinCounter = (spinCounter + 1) % spinnerFrames.length;
        return composeLines(run, PENDING_SUFFIX);
    }

    @Override
    public int refreshIntervalMillis() {
        return REFRESH_INTERVAL_MS;
    }

    @Override
    public List<String> completed() {
        WorkflowRun run = currentRun();
        int count = run.stages().size();
        long total = run.stages().stream().mapToLong(WorkflowRun.Stage::durationMillis).sum();
        return List.of(doneSummaryLine(count, total));
    }

    @Override
    public List<String> failed() {
        return composeLines(currentRun(), SKIPPED_SUFFIX);
    }

    private List<String> composeLines(WorkflowRun run, String pendingSuffix) {
        List<WorkflowRun.Stage> done = new ArrayList<>();
        List<WorkflowRun.Stage> active = new ArrayList<>();
        List<WorkflowRun.Stage> failed = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (WorkflowRun.Stage stage : run.stages()) {
            seen.add(stage.name());
            switch (stage.status()) {
                case STATUS_SUCCESS -> done.add(stage);
                case STATUS_IN_PROGRESS -> active.add(stage);
                case STATUS_FAILED -> failed.add(stage);
                default -> {
                    // unknown statuses (ABORTED, NOT_EXECUTED, FAILURE) are silently dropped — only the three observed statuses are rendered
                }
            }
        }

        List<String> lines = new ArrayList<>();
        if (!done.isEmpty()) {
            long ms = done.stream().mapToLong(WorkflowRun.Stage::durationMillis).sum();
            lines.add(doneSummaryLine(done.size(), ms));
        }
        String spinnerIcon = String.valueOf(spinnerFrames[spinCounter]);
        for (WorkflowRun.Stage stage : active) {
            lines.add(percentageBar.running(activePercent(stage), stage.name(), spinnerIcon));
        }
        for (WorkflowRun.Stage stage : failed) {
            lines.add(percentageBar.error(failedPercent(stage), stage.name()));
        }
        List<String> pending = pendingNames(seen);
        if (!pending.isEmpty()) {
            lines.add(pendingLine(pending, pendingSuffix));
        }
        return lines;
    }

    private int activePercent(WorkflowRun.Stage stage) {
        long elapsed = currentTimeMillis() - stage.startTimeMillis();
        return clampedPercent(elapsed, expectedDuration(stage));
    }

    private int failedPercent(WorkflowRun.Stage stage) {
        long elapsed = stage.durationMillis() > 0
            ? stage.durationMillis()
            : Math.max(0, currentTimeMillis() - stage.startTimeMillis());
        return clampedPercent(elapsed, expectedDuration(stage));
    }

    private static int clampedPercent(long elapsed, long expected) {
        long pct = elapsed * 100 / expected;
        if (pct < 0) {
            return 0;
        }
        if (pct > MAX_RUNNING_PERCENT) {
            return MAX_RUNNING_PERCENT;
        }
        return (int) pct;
    }

    private long expectedDuration(WorkflowRun.Stage stage) {
        return getPreviousBuildStages().stream()
            .filter(s -> s.name().equals(stage.name()))
            .findFirst()
            .map(WorkflowRun.Stage::durationMillis)
            .filter(d -> d > 0)
            .orElse(DEFAULT_STAGE_DURATION_MS);
    }

    private List<String> pendingNames(Set<String> seen) {
        return getPreviousBuildStages().stream()
            .map(WorkflowRun.Stage::name)
            .filter(n -> !seen.contains(n))
            .distinct()
            .toList();
    }

    private List<WorkflowRun.Stage> getPreviousBuildStages() {
        if (previousBuildStages == null) {
            previousBuildStages = buildNumber > 1
                ? pipelineAPI.getJobBuildDescription(jobUrl, buildNumber - 1).stages()
                : List.of();
        }
        return previousBuildStages;
    }

    private WorkflowRun currentRun() {
        long now = currentTimeMillis();
        if (latestRun == null || now - latestRunFetchedAtMillis >= FETCH_INTERVAL_MS) {
            latestRun = pipelineAPI.getJobBuildDescription(jobUrl, buildNumber);
            latestRunFetchedAtMillis = now;
        }
        return latestRun;
    }

    private String doneSummaryLine(int count, long durationMs) {
        String word = count == 1 ? "stage" : "stages";
        String text = count + " " + word + " completed (" + formatDuration(durationMs) + ")";
        String icon = percentageBar.doneIcon();
        return icon.isEmpty() ? text : icon + "  " + text;
    }

    private String pendingLine(List<String> names, String suffix) {
        String icon = caps.supportsUnicode() ? theme.dim(symbols.pending()) : "";
        int prefixCols = icon.isEmpty() ? 0 : 3;
        int budget = Math.max(MIN_PENDING_BUDGET, caps.width() - prefixCols - suffix.length());
        String joined = truncateJoined(names, budget);
        String dimmed = theme.dim(joined + suffix);
        return icon.isEmpty() ? dimmed : icon + "  " + dimmed;
    }

    static String truncateJoined(List<String> names, int budget) {
        if (names.isEmpty() || budget <= 0) {
            return "";
        }
        String full = String.join(", ", names);
        if (full.length() <= budget) {
            return full;
        }
        List<String> trimmed = new ArrayList<>(names);
        while (!trimmed.isEmpty()) {
            String candidate = String.join(", ", trimmed) + ", " + ELLIPSIS;
            if (candidate.length() <= budget) {
                return candidate;
            }
            trimmed.removeLast();
        }
        String first = names.getFirst();
        int hardBudget = Math.max(1, budget - ELLIPSIS.length());
        return first.substring(0, Math.min(first.length(), hardBudget)) + ELLIPSIS;
    }

    static String formatDuration(long ms) {
        long safe = Math.max(0, ms);
        long seconds = safe / 1000;
        if (seconds < 60) {
            return seconds + "s";
        }
        long minutes = seconds / 60;
        long remSec = seconds % 60;
        if (minutes < 60) {
            return String.format("%dm %02ds", minutes, remSec);
        }
        long hours = minutes / 60;
        long remMin = minutes % 60;
        return String.format("%dh %02dm", hours, remMin);
    }

    long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
