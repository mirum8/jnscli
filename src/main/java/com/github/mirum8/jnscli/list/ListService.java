package com.github.mirum8.jnscli.list;

import com.github.mirum8.jnscli.common.JobDescriptorProvider;
import com.github.mirum8.jnscli.context.JobType;
import com.github.mirum8.jnscli.context.JobsContext;
import com.github.mirum8.jnscli.jenkins.JenkinsAPI;
import com.github.mirum8.jnscli.jenkins.Job;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.shell.RefreshableMultilineRenderer;
import com.github.mirum8.jnscli.shell.Symbols;
import com.github.mirum8.jnscli.shell.Theme;
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
    private final Theme theme;
    private final Symbols symbols;

    public ListService(JenkinsAPI jenkinsAPI,
                       JobsContext jobsContext,
                       JobListTableFormatter jobListTableFormatter,
                       RefreshableMultilineRenderer refreshableMultilineRenderer,
                       JobDescriptorProvider jobDescriptorProvider,
                       Theme theme,
                       Symbols symbols) {
        this.jenkinsAPI = jenkinsAPI;
        this.jobDescriptorProvider = jobDescriptorProvider;
        this.jobsContext = jobsContext;
        this.jobListTableFormatter = jobListTableFormatter;
        this.refreshableMultilineRenderer = refreshableMultilineRenderer;
        this.theme = theme;
        this.symbols = symbols;
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
                .name(JobType.fromName(job.aClass()) == JobType.FOLDER ? theme.bold(job.name()) : job.name())
                .status(getStatus(job.color(), JobType.fromName(job.aClass())))
                .build())
            .sorted(Comparator.comparingInt(JobRow::id))
            .toList();

        List<String> tableRows = jobListTableFormatter.createJobTable(jobRows);
        refreshableMultilineRenderer.render(tableRows);
    }

    private String getStatus(String color, JobType jobType) {
        if (color == null) {
            return getStatusFromJobType(jobType);
        }
        boolean isRunning = color.endsWith("_anime");
        String base = color.contains("_") ? color.substring(0, color.indexOf('_')) : color;
        String glyph = switch (base) {
            case "blue" -> theme.success(symbols.ok());
            case "red" -> theme.failure(symbols.fail());
            case "yellow" -> theme.warning(symbols.warn());
            case "notbuilt" -> theme.warning(symbols.notbuilt());
            case "disabled" -> theme.warning(symbols.disabled());
            case "aborted" -> theme.warning(symbols.aborted());
            case "grey" -> theme.warning(symbols.unknown());
            default -> " ";
        };
        return isRunning ? glyph + symbols.running() : glyph;
    }

    private String getStatusFromJobType(JobType jobType) {
        return switch (jobType) {
            case FOLDER -> symbols.folder();
            default -> theme.warning(symbols.notbuilt());
        };
    }
}
