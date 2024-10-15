package com.github.mirum8.jnscli.build;

import com.github.mirum8.jnscli.abort.AbortService;
import com.github.mirum8.jnscli.ai.AiService;
import com.github.mirum8.jnscli.build.parameters.ParameterService;
import com.github.mirum8.jnscli.common.JobDescriptorProvider;
import com.github.mirum8.jnscli.context.JobType;
import com.github.mirum8.jnscli.diagnose.ErrorService;
import com.github.mirum8.jnscli.jenkins.*;
import com.github.mirum8.jnscli.jenkins.QueueItem.QueueItemType;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.runner.CommandParameters;
import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.runner.Result;
import com.github.mirum8.jnscli.runner.Spinner;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import com.github.mirum8.jnscli.shell.TextColor;
import com.github.mirum8.jnscli.util.Threads;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.github.mirum8.jnscli.jenkins.Status.*;
import static com.github.mirum8.jnscli.shell.TextFormatter.colored;
import static java.util.stream.Collectors.toMap;

@Service
class BuildService {
    public static final String NOT_ABORT = "Do not abort. Start new build";
    public static final String ABORT_ALL = "Abort all. Start new build";
    public static final String CANCEL_BUILD = "Cancel new build";
    public static final String FINISHED_PREFIX = "Finished: ";

    private final ShellPrinter shellPrinter;
    private final JenkinsAPI jenkinsAPI;
    private final ShellPrompter shellPrompter;
    private final AbortService abortService;
    private final CommandRunner commandRunner;
    private final ParameterService parameterService;
    private final JobDescriptorProvider jobDescriptorProvider;
    private final AiService aiService;
    private final PipelineAPI pipelineAPI;
    private final ErrorService errorService;

    BuildService(ShellPrinter shellPrinter,
                 JenkinsAPI jenkinsAPI,
                 ShellPrompter shellPrompter,
                 AbortService abortService,
                 CommandRunner commandRunner,
                 ParameterService parameterService,
                 JobDescriptorProvider jobDescriptorProvider,
                 AiService aiService,
                 PipelineAPI pipelineAPI,
                 ErrorService errorService) {
        this.shellPrinter = shellPrinter;
        this.jenkinsAPI = jenkinsAPI;
        this.shellPrompter = shellPrompter;
        this.abortService = abortService;
        this.commandRunner = commandRunner;
        this.parameterService = parameterService;
        this.jobDescriptorProvider = jobDescriptorProvider;
        this.aiService = aiService;
        this.pipelineAPI = pipelineAPI;
        this.errorService = errorService;
    }

    void build(String jobId, boolean progress, boolean showLog, List<String> parameters, boolean useAi) {
        JobDescriptor job = jobDescriptorProvider.get(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job " + jobId + " not found"));

        String jobUrl = job.url();
        WorkflowJob workflowJob = jenkinsAPI.getWorkflowJob(jobUrl);
        if (!workflowJob.buildable()) {
            throw new IllegalArgumentException("The job is not buildable");
        }

        if (askWhetherToAbortPreviousBuild(job).equals(CANCEL_BUILD)) {
            shellPrinter.println("Build cancelled");
            return;
        }

        Map<String, String> filledParameters = parameters == null || parameters.isEmpty()
            ? promptParameters(workflowJob, parameters)
            : Map.of();

        Result<Void> result = workflowJob.property().stream()
            .map(WorkflowJob.Property::parameterDefinitions)
            .filter(Objects::nonNull)
            .flatMap(List::stream)
            .filter(parameterDefinition -> parameterDefinition.type().equals("FileParameterDefinition"))
            .findFirst()
            .map(parameterDefinition -> startJobWithFile(job, filledParameters, parameterDefinition))
            .orElseGet(() -> startJob(job, filledParameters));

        int buildNumber = workflowJob.nextBuildNumber();
        if (result instanceof Result.Failure) {
            shellPrinter.println(getErrorMessage(job, buildNumber, useAi));
            shellPrinter.println(FINISHED_PREFIX + jenkinsAPI.getJobBuildInfo(job.url(), buildNumber).status().name());
            return;
        }
        if (progress && !showLog) {
            if (job.type() == JobType.WORKFLOW) {
                commandRunner.showProgress(CommandParameters.<BuildInfo>builder()
                    .withProgressBar(new BuildProgressBar(pipelineAPI, job.url(), buildNumber))
                    .withCompletionChecker(() -> jenkinsAPI.getJobBuildInfo(job.url(), buildNumber))
                    .withSuccessWhen(buildInfo -> buildInfo.status() == Status.SUCCESS)
                    .withFailureWhen(buildInfo -> buildInfo.status() == FAILED || buildInfo.status() == FAILURE || buildInfo.status() == ABORTED)
                    .onSuccess(ignored -> FINISHED_PREFIX + colored(SUCCESS.name(), TextColor.GREEN))
                    .onFailure(ignored -> getErrorMessage(job, buildNumber, useAi))
                    .build());
            } else {
                commandRunner.showProgress(CommandParameters.<WorkflowJob>builder()
                    .withProgressBar(Spinner.builder()
                        .runningMessage("Job " + job.name() + " is running")
                        .completeMessage(FINISHED_PREFIX + colored(SUCCESS.name(), TextColor.GREEN))
                        .errorMessage(FINISHED_PREFIX + colored(FAILED.name(), TextColor.RED) + "/nCheck logs: " + job.url() + "/" + workflowJob.lastBuild() + "/console")
                        .build()
                    )
                    .withCompletionChecker(() -> jenkinsAPI.getWorkflowJob(job.url()))
                    .withSuccessWhen(wj -> wj.color().equals("blue"))
                    .withFailureWhen(wj -> wj.color().equals("red") || wj.color().equals("aborted"))
                    .build());
            }
        }
        if (showLog) {
            showConsoleText(job.url(), buildNumber);
        }
    }

    private String askWhetherToAbortPreviousBuild(JobDescriptor job) {
        if (job.type() != JobType.WORKFLOW) {
            return NOT_ABORT;
        }

        record RunWithBuildInfo(Run run, BuildInfo buildInfo) {
        }

        List<RunWithBuildInfo> runningBuilds = pipelineAPI.getJobRuns(job.url())
            .stream()
            .filter(run -> run.status().equals(IN_PROGRESS))
            .map(run -> new RunWithBuildInfo(run, jenkinsAPI.getJobBuildInfo(job.url(), run.id())))
            .toList();

        if (runningBuilds.isEmpty()) {
            return NOT_ABORT;
        }

        shellPrinter.println("Job " + job.name() + " is already running. Builds:");
        runningBuilds.forEach(build -> shellPrinter.println(colored("  #" + build.run().id(), TextColor.YELLOW) + "\n" +
                colored("  Started by: ", TextColor.CYAN) + build.buildInfo().startedBy().orElse("Unknown") + "\n" +
                colored("  Current stage: ", TextColor.CYAN) + getCurrentStage(build.run()) + "\n" +
                getParameters(build.buildInfo())
            )
        );

        List<String> options = new ArrayList<>();
        options.add(CANCEL_BUILD);
        options.add(NOT_ABORT);
        options.add(ABORT_ALL);

        Map<String, Integer> optionToBuildNumber = runningBuilds.stream().collect(toMap(runWithBuildInfo -> "Abort build " + runWithBuildInfo.run().id() + ". Start new build", runWithBuildInfo -> runWithBuildInfo.run().id()));
        options.addAll(optionToBuildNumber.keySet());

        String chosenOption = shellPrompter.promptSelectFromList("Choose an option", options);

        if (chosenOption.equals(ABORT_ALL)) {
            runningBuilds.forEach(runWithBuildInfo -> abortService.abort(job.name(), runWithBuildInfo.run().id()));
        } else if (chosenOption.equals(CANCEL_BUILD)) {
            return CANCEL_BUILD;
        } else if (!chosenOption.equals(NOT_ABORT)) {
            abortService.abort(job.name(), optionToBuildNumber.get(chosenOption));
        }
        return chosenOption;
    }

    private String getCurrentStage(Run build) {
        return build.stages().stream()
            .filter(stage -> stage.status() == IN_PROGRESS)
            .findFirst()
            .map(Run.Stage::name)
            .orElse("Unknown");
    }

    private String getParameters(BuildInfo build) {
        return build.parameters().stream()
            .map(parameter -> "  " + colored(parameter.name(), TextColor.CYAN) + ": " + parameter.value())
            .collect(Collectors.joining("\n"));
    }

    private void showConsoleText(String jobUrl, int buildNumber) {
        long start = 0;
        ProgressiveConsoleText progressiveConsoleText;
        do {
            progressiveConsoleText = jenkinsAPI.getProgressiveConsoleText(jobUrl, buildNumber, start);
            shellPrinter.print(progressiveConsoleText.text());
            start = progressiveConsoleText.nextStart();
            Threads.sleepSecs(3);
        } while (progressiveConsoleText.hasMoreData());
    }

    private Map<String, String> promptParameters(WorkflowJob workflowJob, List<String> parameters) {
        return parameterService.prompt(workflowJob, parameters);
    }

    private String getErrorMessage(JobDescriptor job, int buildNumber, boolean useAi) {
        String errors = errorService.getErrors(job, buildNumber);
        return useAi ? colored("AI analysis: ", TextColor.MAGENTA) + aiService.analyzeLog(errors) : "Errors:\n" + errors;
    }

    private Result<Void> startJob(JobDescriptor job, Map<String, String> parameters) {
        String jobUrl = job.url();
        QueueItemLocation queueItemLocation = parameters != null && !parameters.isEmpty()
            ? jenkinsAPI.runJob(jobUrl, parameters.entrySet().stream().map(e -> e.getKey() + "=" + e.getValue()).toList())
            : jenkinsAPI.runJob(jobUrl);
        return showStartingJobStatus(job, queueItemLocation);
    }

    private Result<Void> startJobWithFile(JobDescriptor job, Map<String, String> filledParameters, WorkflowJob.Property.ParameterDefinition fileParameter) {
        String fileName = fileParameter.name();
        String filePath = filledParameters.get(fileName);
        Objects.requireNonNull(filePath, "File parameter " + fileName + " is not filled");
        List<String> parameters = filledParameters.entrySet().stream()
            .filter(e -> !e.getKey().equals(fileName))
            .map(e -> e.getKey() + "=" + e.getValue())
            .toList();
        QueueItemLocation queueItemLocation = jenkinsAPI.runJobWithFileParam(job.url(), fileName, Path.of(filePath), parameters);
        return showStartingJobStatus(job, queueItemLocation);
    }

    private Result<Void> showStartingJobStatus(JobDescriptor job, QueueItemLocation queueItemLocation) {
        return commandRunner.showProgress(CommandParameters.<QueueItem>builder()
            .withProgressBar(Spinner.builder().runningMessage("Starting job " + job.name())
                .completeMessage("Job " + job.name() + " started")
                .build())
            .withCompletionChecker(() -> jenkinsAPI.getQueueItem(queueItemLocation.url()))
            .withSuccessWhen(queueItem -> queueItem != null && QueueItemType.LEFT_ITEM == queueItem.type())
            .withTimeout(90)
            .onTimeoutError(() -> colored("✗ ", TextColor.RED) + "Job " + job.name() + " failed to start.")
            .build());
    }

}
