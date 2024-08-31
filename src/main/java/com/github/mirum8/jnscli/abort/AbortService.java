package com.github.mirum8.jnscli.abort;

import com.github.mirum8.jnscli.common.JobDescriptorProvider;
import com.github.mirum8.jnscli.jenkins.BuildInfo;
import com.github.mirum8.jnscli.jenkins.JenkinsAPI;
import com.github.mirum8.jnscli.jenkins.Status;
import com.github.mirum8.jnscli.jenkins.WorkflowJob;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.runner.OperationParameters;
import com.github.mirum8.jnscli.runner.Spinner;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import org.springframework.stereotype.Service;

import static com.github.mirum8.jnscli.shell.TextColor.GREEN;
import static com.github.mirum8.jnscli.shell.TextFormatter.colored;

@Service
public class AbortService {
    private final JenkinsAPI jenkinsAPI;
    private final ShellPrinter shellPrinter;
    private final CommandRunner commandRunner;
    private final JobDescriptorProvider jobDescriptorProvider;

    public AbortService(JenkinsAPI jenkinsAPI, ShellPrinter shellPrinter, CommandRunner commandRunner, JobDescriptorProvider jobDescriptorProvider) {
        this.jenkinsAPI = jenkinsAPI;
        this.shellPrinter = shellPrinter;
        this.commandRunner = commandRunner;
        this.jobDescriptorProvider = jobDescriptorProvider;
    }

    public void abort(String jobId) {
        JobDescriptor job = jobDescriptorProvider.get(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job " + jobId + " not found"));

        WorkflowJob workflowJob = jenkinsAPI.getWorkflowJob(job.url());
        if (!workflowJob.isRunning()) {
            shellPrinter.println("Job " + jobId + " is not running");
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
            shellPrinter.println("Job " + jobId + " is not running");
            return;
        }

        abort(job, buildNumber);
    }

    private void abort(JobDescriptor job, int buildNumber) {
        jenkinsAPI.abortJob(job.url(), buildNumber);

        OperationParameters<BuildInfo> parameters = OperationParameters.<BuildInfo>builder()
            .withProgressBar(new Spinner("Aborting job " + job.name()))
            .withCompletionChecker(() -> jenkinsAPI.getJobBuildInfo(job.url(), buildNumber))
            .withSuccessWhen(workflowRun -> workflowRun.status() == Status.ABORTED)
            .onSuccess(ignored -> colored("✓ ", GREEN) + "Job " + job.name() + " aborted")
            .withTimeout(60)
            .build();

        commandRunner.start(() -> jenkinsAPI.abortJob(job.url(), buildNumber), parameters);
    }
}
