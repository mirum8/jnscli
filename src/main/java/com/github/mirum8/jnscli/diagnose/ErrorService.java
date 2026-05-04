package com.github.mirum8.jnscli.diagnose;

import com.github.mirum8.jnscli.ai.AiService;
import com.github.mirum8.jnscli.common.JobDescriptorProvider;
import com.github.mirum8.jnscli.context.JobType;
import com.github.mirum8.jnscli.jenkins.*;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.settings.SettingsService;
import com.github.mirum8.jnscli.shell.JsonOutput;
import com.github.mirum8.jnscli.shell.Messages;
import com.github.mirum8.jnscli.shell.OutputContext;
import com.github.mirum8.jnscli.shell.Section;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.shell.Theme;
import com.github.mirum8.jnscli.util.StatusFormatter;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

@Component
public class ErrorService {
    private final AiService aiService;
    private final JenkinsAPI jenkinsAPI;
    private final PipelineAPI pipelineAPI;
    private final SettingsService settingsService;
    private final ShellPrinter shellPrinter;
    private final Messages messages;
    private final JobDescriptorProvider jobDescriptorProvider;
    private final CommandRunner commandRunner;
    private final Section section;
    private final Theme theme;
    private final StatusFormatter statusFormatter;
    private final OutputContext outputContext;
    private final JsonOutput jsonOutput;

    public ErrorService(AiService aiService,
                        JenkinsAPI jenkinsAPI,
                        PipelineAPI pipelineAPI,
                        SettingsService settingsService,
                        ShellPrinter shellPrinter,
                        Messages messages,
                        JobDescriptorProvider jobDescriptorProvider,
                        CommandRunner commandRunner,
                        Section section,
                        Theme theme,
                        StatusFormatter statusFormatter,
                        OutputContext outputContext,
                        JsonOutput jsonOutput) {
        this.aiService = aiService;
        this.jenkinsAPI = jenkinsAPI;
        this.pipelineAPI = pipelineAPI;
        this.settingsService = settingsService;
        this.shellPrinter = shellPrinter;
        this.messages = messages;
        this.jobDescriptorProvider = jobDescriptorProvider;
        this.commandRunner = commandRunner;
        this.section = section;
        this.theme = theme;
        this.statusFormatter = statusFormatter;
        this.outputContext = outputContext;
        this.jsonOutput = jsonOutput;
    }

    public record ErrorJson(int buildNumber, String status, String startedBy, String errors, String aiAnalysis) {
    }

    public void getError(String jobId, Integer buildNumber, boolean myBuild, boolean useAi) {
        JobDescriptor job = jobDescriptorProvider.get(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job " + jobId + " not found"));

        if (outputContext.isJson()) {
            getErrorJson(job, buildNumber, myBuild, useAi);
            return;
        }
        getErrorText(job, buildNumber, myBuild, useAi);
    }

    private void getErrorJson(JobDescriptor job, Integer buildNumber, boolean myBuild, boolean useAi) {
        BuildInfo buildInfo = resolveBuildInfo(job, buildNumber, myBuild);
        if (buildInfo == null) {
            jsonOutput.println(new ErrorJson(0, null, null, null, null));
            return;
        }
        if (buildInfo.status() == Status.SUCCESS) {
            jsonOutput.println(new ErrorJson(buildInfo.number(), Status.SUCCESS.name(),
                buildInfo.startedBy().orElse(null), null, null));
            return;
        }
        String errorsRaw = getErrors(job, buildInfo.number());
        String analysis = (useAi && !errorsRaw.isEmpty()) ? aiService.analyzeLog(errorsRaw) : null;
        jsonOutput.println(new ErrorJson(
            buildInfo.number(),
            buildInfo.status() == null ? null : buildInfo.status().name(),
            buildInfo.startedBy().orElse(null),
            errorsRaw.isEmpty() ? null : errorsRaw,
            analysis));
    }

    private void getErrorText(JobDescriptor job, Integer buildNumber, boolean myBuild, boolean useAi) {
        BuildInfo buildInfo = resolveBuildInfo(job, buildNumber, myBuild);
        if (buildInfo == null) {
            messages.empty("No build found for the given criteria.");
            return;
        }
        if (buildInfo.status() == Status.SUCCESS) {
            messages.info("Build " + buildInfo.number() + " was successful.");
            return;
        }
        String header = section.builder()
            .header("Build #" + buildInfo.number())
            .field("Started By", buildInfo.startedBy().orElse("Unknown"))
            .field("Status", statusFormatter.colored(buildInfo.status()))
            .build();
        shellPrinter.println(header);

        String errors = commandRunner.callWithSpinner("Fetching errors", () -> getErrors(job, buildInfo.number())).value();
        if (errors.isEmpty()) {
            messages.empty("No errors found.");
            return;
        }
        String errorsText = useAi ? theme.accent("AI analysis: ") + aiService.analyzeLog(errors) : errors;
        shellPrinter.println(errorsText);
    }

    private BuildInfo resolveBuildInfo(JobDescriptor job, Integer buildNumber, boolean myBuild) {
        if (buildNumber != null) {
            return jenkinsAPI.getJobBuildInfo(job.url(), buildNumber);
        }
        return myBuild ? findLatestBuildByCurrentUser(job) : findLastFailedBuild(job);
    }

    private BuildInfo findLatestBuildByCurrentUser(JobDescriptor job) {
        String currentUser = settingsService.readSettings().username();
        if (job.type() == JobType.WORKFLOW) {
            List<Run> runs = pipelineAPI.getJobRuns(job.url());
            return runs.stream()
                .sorted(Comparator.comparingInt(Run::id).reversed())
                .limit(5)
                .map(run -> jenkinsAPI.getJobBuildInfo(job.url(), run.id()))
                .filter(run -> run.status() != Status.SUCCESS)
                .filter(buildInfo -> buildInfo.startedBy().map(startedBy -> startedBy.equals(currentUser)).orElse(false))
                .findFirst().orElse(null);
        } else {
            WorkflowJob workflowJob = jenkinsAPI.getWorkflowJob(job.url());
            return workflowJob.builds().stream()
                .sorted(Comparator.comparingInt(WorkflowJob.Build::number).reversed())
                .limit(5)
                .map(build -> jenkinsAPI.getJobBuildInfo(job.url(), build.number()))
                .filter(buildInfo -> buildInfo.status() != Status.SUCCESS)
                .filter(buildInfo -> buildInfo.startedBy().map(startedBy -> startedBy.equals(currentUser)).orElse(false))
                .findFirst().orElse(null);
        }
    }

    private BuildInfo findLastFailedBuild(JobDescriptor job) {
        if (job.type() == JobType.WORKFLOW) {
            List<Run> runs = pipelineAPI.getJobRuns(job.url());
            return runs.stream()
                .sorted(Comparator.comparingInt(Run::id).reversed())
                .limit(5)
                .map(run -> jenkinsAPI.getJobBuildInfo(job.url(), run.id()))
                .filter(buildInfo -> buildInfo.status() != Status.SUCCESS)
                .findFirst()
                .orElse(null);
        } else {
            WorkflowJob workflowJob = jenkinsAPI.getWorkflowJob(job.url());
            return workflowJob.builds().stream()
                .sorted(Comparator.comparingInt(WorkflowJob.Build::number).reversed())
                .limit(5)
                .map(build -> jenkinsAPI.getJobBuildInfo(job.url(), build.number()))
                .filter(buildInfo -> buildInfo.status() != Status.SUCCESS)
                .findFirst()
                .orElse(null);
        }
    }

    public String getErrors(JobDescriptor job, int buildNumber) {
        if (job.type() == JobType.WORKFLOW) {
            WorkflowRun workflowRun = pipelineAPI.getJobBuildDescription(job.url(), buildNumber);
            if (workflowRun.stages().isEmpty()) {
                return jenkinsAPI.getConsoleText(job.url(), buildNumber);
            }
            return workflowRun.stages().stream()
                .filter(stage -> !stage.status().equals(Status.SUCCESS.name()))
                .findFirst()
                .flatMap(stage -> pipelineAPI.getStageDescription(job.url(), workflowRun.id(), stage.id())
                    .stageFlowNodes().stream()
                    .filter(stageFlowNode -> !stageFlowNode.status().equals(Status.SUCCESS.name()))
                    .findFirst()
                    .map(stageFlowNode -> pipelineAPI.getNodeLog(job.url(), workflowRun.id(), stageFlowNode.id()).text()))
                .orElse(jenkinsAPI.getConsoleText(job.url(), buildNumber));
        } else {
            return jenkinsAPI.getConsoleText(job.url(), buildNumber);
        }
    }

}
