package com.github.mirum8.jnscli.diagnose;

import com.github.mirum8.jnscli.ai.AiService;
import com.github.mirum8.jnscli.common.JobDescriptorProvider;
import com.github.mirum8.jnscli.context.JobType;
import com.github.mirum8.jnscli.jenkins.*;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.settings.SettingsService;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;

import static com.github.mirum8.jnscli.shell.TextColor.CYAN;
import static com.github.mirum8.jnscli.shell.TextColor.MAGENTA;
import static com.github.mirum8.jnscli.shell.TextFormatter.colored;
import static com.github.mirum8.jnscli.util.Statuses.getColored;

@Component
public class ErrorService {
    private final AiService aiService;
    private final JenkinsAPI jenkinsAPI;
    private final PipelineAPI pipelineAPI;
    private final SettingsService settingsService;
    private final ShellPrinter shellPrinter;
    private final JobDescriptorProvider jobDescriptorProvider;

    public ErrorService(AiService aiService,
                        JenkinsAPI jenkinsAPI,
                        PipelineAPI pipelineAPI,
                        SettingsService settingsService,
                        ShellPrinter shellPrinter,
                        JobDescriptorProvider jobDescriptorProvider) {
        this.aiService = aiService;
        this.jenkinsAPI = jenkinsAPI;
        this.pipelineAPI = pipelineAPI;
        this.settingsService = settingsService;
        this.shellPrinter = shellPrinter;
        this.jobDescriptorProvider = jobDescriptorProvider;
    }

    public void getError(String jobId, Integer buildNumber, boolean myBuild, boolean useAi) {
        JobDescriptor job = jobDescriptorProvider.get(jobId)
                .orElseThrow(() -> new IllegalArgumentException("Job " + jobId + " not found"));

        BuildInfo buildInfo;
        if (buildNumber != null) {
            buildInfo = jenkinsAPI.getJobBuildInfo(job.url(), buildNumber);
            if (buildInfo.status() == Status.SUCCESS) {
                shellPrinter.println("Build " + buildNumber + " was successful.");
                return;
            }
        } else if (myBuild) {
            buildInfo = findLatestBuildByCurrentUser(job);
        } else {
            buildInfo = findLastFailedBuild(job);
        }

        if (buildInfo == null) {
            shellPrinter.println("No build found for the given criteria.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(colored("Build Number: ", CYAN)).append(buildInfo.number()).append("\n");
        sb.append(colored("Started By: ", CYAN)).append(buildInfo.startedBy().orElse("Unknown")).append("\n");
        sb.append(colored("Status: ", CYAN)).append(getColored(buildInfo.status())).append("\n");

        String errors = getErrors(job, buildInfo.number());

        if (!errors.isEmpty()) {
            sb.append(useAi ? colored("AI analysis: ", MAGENTA) + aiService.analyzeLog(errors) : colored("Errors: ", CYAN) + errors);
        } else {
            sb.append("No errors found.\n");
        }

        shellPrinter.println(sb.toString());
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
