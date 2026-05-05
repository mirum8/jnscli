package com.github.mirum8.jnscli.abort;

import com.github.mirum8.jnscli.common.JobDescriptorProvider;
import com.github.mirum8.jnscli.jenkins.BuildInfo;
import com.github.mirum8.jnscli.jenkins.JenkinsAPI;
import com.github.mirum8.jnscli.jenkins.Status;
import com.github.mirum8.jnscli.jenkins.WorkflowJob;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.runner.CommandParameters;
import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.runner.SpinnerFactory;
import com.github.mirum8.jnscli.shell.JsonOutput;
import com.github.mirum8.jnscli.shell.Messages;
import com.github.mirum8.jnscli.shell.OutputContext;
import org.springframework.stereotype.Service;

@Service
public class AbortService {
    private final JenkinsAPI jenkinsAPI;
    private final Messages messages;
    private final CommandRunner commandRunner;
    private final JobDescriptorProvider jobDescriptorProvider;
    private final SpinnerFactory spinnerFactory;
    private final OutputContext outputContext;
    private final JsonOutput jsonOutput;

    public AbortService(JenkinsAPI jenkinsAPI,
                        Messages messages,
                        CommandRunner commandRunner,
                        JobDescriptorProvider jobDescriptorProvider,
                        SpinnerFactory spinnerFactory,
                        OutputContext outputContext,
                        JsonOutput jsonOutput) {
        this.jenkinsAPI = jenkinsAPI;
        this.messages = messages;
        this.commandRunner = commandRunner;
        this.jobDescriptorProvider = jobDescriptorProvider;
        this.spinnerFactory = spinnerFactory;
        this.outputContext = outputContext;
        this.jsonOutput = jsonOutput;
    }

    public record AbortJson(String job, int buildNumber, String status) {
    }

    public void abort(String jobId) {
        JobDescriptor job = jobDescriptorProvider.get(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job " + jobId + " not found"));

        WorkflowJob workflowJob = jenkinsAPI.getWorkflowJob(job.url());
        if (!workflowJob.isRunning()) {
            notRunning(job, 0);
            return;
        }

        int lastBuildNumber = workflowJob.lastBuild().number();
        abort(job, lastBuildNumber);
    }

    public void abort(String jobId, int buildNumber) {
        JobDescriptor job = jobDescriptorProvider.get(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job " + jobId + " not found"));

        WorkflowJob workflowJob = jenkinsAPI.getWorkflowJob(job.url());
        if (!workflowJob.isRunning()) {
            notRunning(job, buildNumber);
            return;
        }

        abort(job, buildNumber);
    }

    private void notRunning(JobDescriptor job, int buildNumber) {
        if (outputContext.isJson()) {
            jsonOutput.println(new AbortJson(job.name(), buildNumber, "NOT_RUNNING"));
        } else {
            messages.warning("Job " + job.name() + " is not running");
        }
    }

    private void abort(JobDescriptor job, int buildNumber) {
        if (outputContext.isJson()) {
            jenkinsAPI.abortJob(job.url(), buildNumber);
            BuildInfo info = jenkinsAPI.getJobBuildInfo(job.url(), buildNumber);
            String status = info.status() == null ? "ABORT_REQUESTED" : info.status().name();
            jsonOutput.println(new AbortJson(job.name(), buildNumber, status));
            return;
        }

        CommandParameters<BuildInfo> parameters = CommandParameters.<BuildInfo>builder()
            .withProgressBar(spinnerFactory.builder()
                .runningMessage("Aborting job " + job.name())
                .completeMessage("Job " + job.name() + " aborted")
                .errorMessage("Failed to abort job " + job.name())
                .build())
            .withCompletionChecker(() -> jenkinsAPI.getJobBuildInfo(job.url(), buildNumber))
            .withSuccessWhen(workflowRun -> workflowRun.status() == Status.ABORTED)
            .withTimeout(60)
            .build();

        commandRunner.run(() -> jenkinsAPI.abortJob(job.url(), buildNumber), parameters);
    }
}
