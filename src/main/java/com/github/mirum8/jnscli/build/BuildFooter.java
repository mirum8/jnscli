package com.github.mirum8.jnscli.build;

import com.github.mirum8.jnscli.jenkins.JenkinsAPI;
import com.github.mirum8.jnscli.jenkins.NodeDescription;
import com.github.mirum8.jnscli.jenkins.PipelineAPI;
import com.github.mirum8.jnscli.jenkins.ProgressiveConsoleText;
import com.github.mirum8.jnscli.jenkins.StageDescription;
import com.github.mirum8.jnscli.jenkins.WorkflowRun;
import com.github.mirum8.jnscli.shell.TerminalCapabilities;
import com.github.mirum8.jnscli.shell.Theme;
import org.jline.utils.AttributedString;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.regex.Pattern;

class BuildFooter {
    private static final int LOG_LINES_CAPACITY = 5;
    private static final long STEP_FETCH_INTERVAL_MS = 5000L;
    private static final long LOG_FETCH_INTERVAL_MS = 3000L;
    private static final String IN_PROGRESS = "IN_PROGRESS";
    private static final String ELLIPSIS = "…";
    private static final String INDENT = "  ";
    private static final Pattern ERROR_PATTERN = Pattern.compile("\\b(ERROR|Exception|FAILED|FAILURE)\\b");
    private static final Pattern WARN_PATTERN = Pattern.compile("\\bWARN(?:ING)?\\b");
    private static final Pattern TRAILING_WHITESPACE = Pattern.compile("\\s+$");

    private final PipelineAPI pipelineAPI;
    private final JenkinsAPI jenkinsAPI;
    private final TerminalCapabilities caps;
    private final Theme theme;
    private final String jobUrl;
    private final int buildNumber;
    private final LongSupplier clock;

    private final Deque<String> logLines = new ArrayDeque<>();
    private long lastLogStartByte;
    private long lastStepFetchedAtMillis;
    private boolean hasFetchedStep;
    private long lastLogFetchedAtMillis;
    private boolean hasFetchedLog;
    private String activeStageName;
    private String activeStepName;
    private String activeStepCommand;

    BuildFooter(PipelineAPI pipelineAPI, JenkinsAPI jenkinsAPI,
                TerminalCapabilities caps, Theme theme,
                String jobUrl, int buildNumber, LongSupplier clock) {
        this.pipelineAPI = pipelineAPI;
        this.jenkinsAPI = jenkinsAPI;
        this.caps = caps;
        this.theme = theme;
        this.jobUrl = jobUrl;
        this.buildNumber = buildNumber;
        this.clock = clock;
    }

    void refresh(WorkflowRun run) {
        long now = clock.getAsLong();
        if (!hasFetchedStep || now - lastStepFetchedAtMillis >= STEP_FETCH_INTERVAL_MS) {
            hasFetchedStep = true;
            lastStepFetchedAtMillis = now;
            refreshActiveStep(run);
        }
        if (!hasFetchedLog || now - lastLogFetchedAtMillis >= LOG_FETCH_INTERVAL_MS) {
            hasFetchedLog = true;
            lastLogFetchedAtMillis = now;
            refreshLog();
        }
    }

    List<String> render() {
        boolean hasHeader = activeStageName != null;
        boolean hasLog = !logLines.isEmpty();
        if (!hasHeader && !hasLog) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        out.add("");
        if (hasHeader) {
            out.add(headerLine());
        }
        if (hasLog) {
            renderLogLines(out);
        }
        return out;
    }

    private void refreshActiveStep(WorkflowRun run) {
        WorkflowRun.Stage stage = run.stages().stream()
            .filter(s -> IN_PROGRESS.equals(s.status()))
            .max(Comparator.comparingLong(WorkflowRun.Stage::startTimeMillis))
            .orElse(null);
        if (stage == null) {
            activeStageName = null;
            activeStepName = null;
            activeStepCommand = null;
            return;
        }
        activeStageName = stage.name();
        StageDescription stageDescription = pipelineAPI.getStageDescription(jobUrl, buildNumber, stage.id());
        StageDescription.FlowNode activeNode = lastInProgressNode(stageDescription);
        if (activeNode == null) {
            activeStepName = null;
            activeStepCommand = null;
            return;
        }
        NodeDescription nodeDescription = pipelineAPI.getNodeDescription(jobUrl, buildNumber, activeNode.id());
        activeStepName = blankToNull(nodeDescription.name());
        activeStepCommand = blankToNull(nodeDescription.parameterDescription());
    }

    private static StageDescription.FlowNode lastInProgressNode(StageDescription stageDescription) {
        if (stageDescription == null || stageDescription.stageFlowNodes() == null) {
            return null;
        }
        StageDescription.FlowNode latest = null;
        for (StageDescription.FlowNode node : stageDescription.stageFlowNodes()) {
            if (IN_PROGRESS.equals(node.status())) {
                latest = node;
            }
        }
        return latest;
    }

    private void refreshLog() {
        ProgressiveConsoleText delta = jenkinsAPI.getProgressiveConsoleText(jobUrl, buildNumber, lastLogStartByte);
        lastLogStartByte = delta.nextStart();
        String body = delta.text();
        if (body == null || body.isEmpty()) {
            return;
        }
        for (String raw : body.split("\n")) {
            String stripped = AttributedString.fromAnsi(raw).toString();
            String trimmed = TRAILING_WHITESPACE.matcher(stripped).replaceAll("");
            if (trimmed.isEmpty()) {
                continue;
            }
            if (logLines.size() >= LOG_LINES_CAPACITY) {
                logLines.removeFirst();
            }
            logLines.addLast(trimmed);
        }
    }

    private String headerLine() {
        StringBuilder sb = new StringBuilder();
        sb.append(INDENT);
        sb.append(theme.dim(unicodeAware("▸", ">")));
        sb.append(' ');
        sb.append(activeStageName);
        if (activeStepName != null) {
            sb.append(theme.dim(unicodeAware(" › ", " > ")));
            sb.append(activeStepName);
            if (activeStepCommand != null) {
                sb.append("  ");
                sb.append(theme.dim(activeStepCommand));
            }
        }
        return sb.toString();
    }

    private void renderLogLines(List<String> out) {
        String prefix = unicodeAware("│ ", "| ");
        int budget = Math.max(0, caps.width() - INDENT.length() - prefix.length());
        List<String> snapshot = new ArrayList<>(logLines);
        int last = snapshot.size() - 1;
        for (int i = 0; i < snapshot.size(); i++) {
            String body = tailTruncate(snapshot.get(i), budget);
            out.add(INDENT + theme.dim(prefix) + colorise(body, i == last));
        }
    }

    private String colorise(String body, boolean newest) {
        if (ERROR_PATTERN.matcher(body).find()) {
            return theme.failure(body);
        }
        if (WARN_PATTERN.matcher(body).find()) {
            return theme.warning(body);
        }
        return newest ? body : theme.dim(body);
    }

    static String tailTruncate(String s, int budget) {
        if (budget <= 0) {
            return "";
        }
        if (s.length() <= budget) {
            return s;
        }
        int keep = Math.max(0, budget - ELLIPSIS.length());
        return ELLIPSIS + s.substring(s.length() - keep);
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private String unicodeAware(String unicode, String ascii) {
        return caps.supportsUnicode() ? unicode : ascii;
    }
}
