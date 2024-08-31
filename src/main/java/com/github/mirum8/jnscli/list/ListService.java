package com.github.mirum8.jnscli.list;

import com.github.mirum8.jnscli.common.JobDescriptorProvider;
import com.github.mirum8.jnscli.context.JobType;
import com.github.mirum8.jnscli.context.JobsContext;
import com.github.mirum8.jnscli.jenkins.JenkinsAPI;
import com.github.mirum8.jnscli.jenkins.Job;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.shell.RefreshableMultilineRenderer;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ListService {
    private final JenkinsAPI jenkinsAPI;
    private final JobsContext jobsContext;
    private final JobListTableFormatter jobListTableFormatter;
    private final RefreshableMultilineRenderer refreshableMultilineRenderer;
    private final JobDescriptorProvider jobDescriptorProvider;

    public ListService(JenkinsAPI jenkinsAPI,
                       JobsContext jobsContext,
                       JobListTableFormatter jobListTableFormatter,
                       RefreshableMultilineRenderer refreshableMultilineRenderer,
                       JobDescriptorProvider jobDescriptorProvider) {
        this.jenkinsAPI = jenkinsAPI;
        this.jobDescriptorProvider = jobDescriptorProvider;
        this.jobsContext = jobsContext;
        this.jobListTableFormatter = jobListTableFormatter;
        this.refreshableMultilineRenderer = refreshableMultilineRenderer;
    }

    public void listJobs() {
        List<Job> jobs = jenkinsAPI.getJobs();
        jobsContext.refreshJobIds(jobs, false);
        renderJobList(jobs);
    }

    public void listJobs(String folderId) {
        JobDescriptor jobDescriptor = jobDescriptorProvider.get(folderId)
            .orElseThrow(() -> new IllegalArgumentException("Folder " + folderId + " not found"));
        if (jobDescriptor.type() != JobType.FOLDER) {
            throw new IllegalArgumentException("Job with id " + folderId + " is not a folder");
        }
        List<Job> jobs = jenkinsAPI.getFolderJobs(jobDescriptor.url()).jobs().stream()
            .map(job -> job.copyWithName(jobDescriptor.name() + "/" + job.name()))
            .toList();
        jobsContext.refreshJobIds(jobs, true);
        renderJobList(jobs);
    }

    private void renderJobList(List<Job> jobs) {
        List<JobRow> jobRows = jobs.stream()
            .map(job -> JobRow.builder()
                .id(jobsContext.findJobByName(job.name()).map(JobDescriptor::id).orElse(0))
                .name(JobType.fromName(job.aClass()) == JobType.FOLDER ? "\33[1m" + job.name() + "\33[0m" : job.name())
                .color(getColor(job.color(), JobType.fromName(job.aClass())))
                .build())
            .sorted(Comparator.comparingInt(JobRow::id))
            .toList();

        List<String> tableRows = jobListTableFormatter.createJobTable(jobRows);
        refreshableMultilineRenderer.render(tableRows);
    }

    private Symbol getColor(String color, JobType jobType) {
        if (color == null) {
            return getColorFromJobType(jobType);
        }
        boolean isRunning = color.endsWith("_anime");
        if (color.contains("_")) {
            color = color.substring(0, color.indexOf('_'));
        }
        Symbol status = switch (color) {
            case "blue" -> new Symbol.Single("✔");
            case "red" -> new Symbol.Single("✘");
            case "yellow" -> new Symbol.Single("!");
            case "notbuilt" -> new Symbol.Single("N");
            case "disabled" -> new Symbol.Single("D");
            case "aborted" -> new Symbol.Single("A");
            case "grey" -> new Symbol.Single("?");
            default -> new Symbol.Single(" ");
        };
        if (isRunning) {
            status = new Symbol.Double(status.value() + "*");
        }
        return status;
    }

    private Symbol getColorFromJobType(JobType jobType) {
        return switch (jobType) {
            case FOLDER -> new Symbol.Double("📁");
            default -> new Symbol.Single("N");
        };
    }
}
