package com.github.mirum8.jnscli.list;

import com.github.mirum8.jnscli.common.JobDescriptorProvider;
import com.github.mirum8.jnscli.context.JobType;
import com.github.mirum8.jnscli.context.JobsContext;
import com.github.mirum8.jnscli.jenkins.JenkinsAPI;
import com.github.mirum8.jnscli.jenkins.Job;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.shell.JsonOutput;
import com.github.mirum8.jnscli.shell.Messages;
import com.github.mirum8.jnscli.shell.OutputContext;
import com.github.mirum8.jnscli.shell.RefreshableMultilineRenderer;
import com.github.mirum8.jnscli.shell.Section;
import com.github.mirum8.jnscli.shell.ShellPrinter;
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
    private final OutputContext outputContext;
    private final JsonOutput jsonOutput;
    private final Messages messages;
    private final Section section;
    private final ShellPrinter shellPrinter;

    public ListService(JenkinsAPI jenkinsAPI,
                       JobsContext jobsContext,
                       JobListTableFormatter jobListTableFormatter,
                       RefreshableMultilineRenderer refreshableMultilineRenderer,
                       JobDescriptorProvider jobDescriptorProvider,
                       Theme theme,
                       Symbols symbols,
                       OutputContext outputContext,
                       JsonOutput jsonOutput,
                       Messages messages,
                       Section section,
                       ShellPrinter shellPrinter) {
        this.jenkinsAPI = jenkinsAPI;
        this.jobDescriptorProvider = jobDescriptorProvider;
        this.jobsContext = jobsContext;
        this.jobListTableFormatter = jobListTableFormatter;
        this.refreshableMultilineRenderer = refreshableMultilineRenderer;
        this.theme = theme;
        this.symbols = symbols;
        this.outputContext = outputContext;
        this.jsonOutput = jsonOutput;
        this.messages = messages;
        this.section = section;
        this.shellPrinter = shellPrinter;
    }

    public record JobJson(int id, String name, String url, String type, String color) {
    }

    public void listJobs() {
        List<Job> jobs = jenkinsAPI.getJobs();
        jobsContext.refreshJobIds(jobs, false);
        renderJobList(jobs, null);
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
        renderJobList(jobs, jobDescriptor.name() + "/");
    }

    private void renderJobList(List<Job> jobs, String breadcrumb) {
        if (outputContext.isJson()) {
            List<JobJson> rows = jobs.stream()
                .map(job -> new JobJson(
                    jobsContext.findJobByName(job.name()).map(JobDescriptor::id).orElse(0),
                    job.name(),
                    job.url(),
                    JobType.fromName(job.aClass()).name(),
                    job.color()))
                .sorted(Comparator.comparingInt(JobJson::id))
                .toList();
            jsonOutput.println(rows);
            return;
        }

        if (breadcrumb != null) {
            shellPrinter.println(section.builder().header(breadcrumb).build());
        }

        if (jobs.isEmpty()) {
            messages.empty(breadcrumb != null ? "No jobs in " + breadcrumb : "No jobs found.");
            return;
        }

        List<JobRow> jobRows = jobs.stream()
            .map(job -> JobRow.builder()
                .id(jobsContext.findJobByName(job.name()).map(JobDescriptor::id).orElse(0))
                .name(displayName(job.name(), breadcrumb))
                .status(getStatus(job.color(), JobType.fromName(job.aClass())))
                .build())
            .sorted(Comparator.comparingInt(JobRow::id))
            .toList();

        List<String> tableRows = jobListTableFormatter.createJobTable(jobRows);
        refreshableMultilineRenderer.render(tableRows);

        long folderCount = jobs.stream()
            .filter(j -> JobType.fromName(j.aClass()) == JobType.FOLDER)
            .count();
        long jobCount = jobs.size() - folderCount;
        StringBuilder footer = new StringBuilder()
            .append(jobCount).append(" job").append(jobCount == 1 ? "" : "s");
        if (folderCount > 0) {
            footer.append(", ").append(folderCount).append(" folder").append(folderCount == 1 ? "" : "s");
        }
        messages.info(footer.toString());
    }

    private String displayName(String fullName, String breadcrumb) {
        if (breadcrumb != null && fullName.startsWith(breadcrumb)) {
            return fullName.substring(breadcrumb.length());
        }
        return fullName;
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
            case "notbuilt" -> theme.dim(symbols.notbuilt());
            case "disabled" -> theme.dim(symbols.disabled());
            case "aborted" -> theme.warning(symbols.aborted());
            case "grey" -> theme.dim(symbols.unknown());
            default -> " ";
        };
        return isRunning ? glyph + symbols.running() : glyph;
    }

    private String getStatusFromJobType(JobType jobType) {
        return switch (jobType) {
            case FOLDER -> symbols.folder();
            default -> theme.dim(symbols.notbuilt());
        };
    }
}
