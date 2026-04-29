package com.github.mirum8.jnscli.info;

import com.github.mirum8.jnscli.common.JobDescriptorProvider;
import com.github.mirum8.jnscli.jenkins.*;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.runner.Result;
import com.github.mirum8.jnscli.settings.SettingsService;
import com.github.mirum8.jnscli.shell.Section;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.util.StatusFormatter;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class InfoService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JenkinsAPI jenkinsAPI;
    private final ShellPrinter shellPrinter;
    private final JobDescriptorProvider jobDescriptorProvider;
    private final String userName;
    private final PipelineAPI pipelineAPI;
    private final CommandRunner commandRunner;
    private final Section section;
    private final StatusFormatter statusFormatter;

    public InfoService(JenkinsAPI jenkinsAPI, ShellPrinter shellPrinter, JobDescriptorProvider jobDescriptorProvider, SettingsService settingsService, PipelineAPI pipelineAPI, CommandRunner commandRunner, Section section, StatusFormatter statusFormatter) {
        this.jenkinsAPI = jenkinsAPI;
        this.shellPrinter = shellPrinter;
        this.jobDescriptorProvider = jobDescriptorProvider;
        this.userName = settingsService.readSettings().username();
        this.pipelineAPI = pipelineAPI;
        this.commandRunner = commandRunner;
        this.section = section;
        this.statusFormatter = statusFormatter;
    }

    public void info(String jobId,
                     Integer buildNumber,
                     boolean includeSuccess,
                     boolean includeFailed,
                     boolean includeRunning, Integer limit, boolean onlyMyBuilds) {
        JobDescriptor job = jobDescriptorProvider.get(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job " + jobId + " not found"));

        if (buildNumber != null) {
            Result<String> result = commandRunner.callWithSpinner("Fetching build info...", () -> fetchBuildInfo(includeBuildStatuses(includeSuccess, includeFailed, includeRunning), limit, onlyMyBuilds, job, null));
            shellPrinter.println(result.value());
        } else {
            Result<WorkflowJob> workflowJobResult = commandRunner.callWithSpinner("Fetching job info...", () -> jenkinsAPI.getWorkflowJob(job.url()));
            printGeneralJobInfo(job, workflowJobResult.value());
            Result<String> result = commandRunner.callWithSpinner("Fetching builds...", () -> fetchBuildInfo(includeBuildStatuses(includeSuccess, includeFailed, includeRunning), limit, onlyMyBuilds, job, workflowJobResult.value()));
            shellPrinter.println(result.value());
        }
    }

    private Set<Status> includeBuildStatuses(boolean includeSuccess, boolean includeFailed, boolean includeRunning) {
        if (!includeSuccess && !includeFailed && !includeRunning) {
            return Arrays.stream(Status.values()).collect(Collectors.toSet());
        }

        Set<Status> statuses = new HashSet<>();
        if (includeSuccess) {
            statuses.add(Status.SUCCESS);
        }
        if (includeFailed) {
            statuses.add(Status.FAILED);
            statuses.add(Status.FAILURE);
        }
        if (includeRunning) {
            statuses.add(Status.IN_PROGRESS);
        }
        return statuses;
    }

    private String fetchWorkflowJobBuilds(JobDescriptor job, Set<Status> statuses, int limit, boolean onlyMyBuilds) {
        record RunWithBuildInfo(Run run, BuildInfo buildInfo) {
        }

        List<Run> builds = pipelineAPI.getJobRuns(job.url());
        List<RunWithBuildInfo> filteredBuilds = builds.stream()
            .filter(build -> statuses.contains(build.status()))
            .map(run -> new RunWithBuildInfo(run, jenkinsAPI.getJobBuildInfo(job.url(), run.id())))
            .sorted(Comparator.<RunWithBuildInfo>comparingInt(r -> r.buildInfo.number()).reversed())
            .filter(r -> !onlyMyBuilds || r.buildInfo().startedBy().map(userName::equals).orElse(false))
            .limit(limit)
            .toList();

        Section.Builder b = section.builder().header("Last builds");
        if (filteredBuilds.isEmpty()) {
            b.line("  No builds found.");
        } else {
            for (RunWithBuildInfo filteredBuild : filteredBuilds) {
                b.divider();
                appendBuildSummary(b, filteredBuild.run(), filteredBuild.buildInfo());
            }
        }
        return b.build();
    }

    private void printGeneralJobInfo(JobDescriptor job, WorkflowJob wj) {
        Section.Builder b = section.builder()
            .header("Job Information")
            .field("Name", wj.name())
            .field("URL", wj.url());
        if (job.alias() != null) {
            b.field("Alias", job.alias());
        }
        if (wj.description() != null && !wj.description().trim().isEmpty()) {
            b.field("Description", wj.description());
        }
        if (wj.property() != null && !wj.property().isEmpty()) {
            b.line("  Parameters:");
            wj.property().stream().map(WorkflowJob.Property::parameterDefinitions).filter(Objects::nonNull)
                .flatMap(List::stream)
                .forEach(parameter -> b.line("    " + parameter.name() + ": " + parameter.defaultValue()));
        }
        shellPrinter.println(b.build());
    }

    private void appendBuildSummary(Section.Builder b, Build run, BuildInfo build) {
        b.header("Build " + build.displayName())
            .field("Status", statusFormatter.colored(run.status()))
            .field("StartedAt", formatTimestamp(build.timestamp()))
            .field("Duration", formatDuration(build.duration()));
        build.startedBy().ifPresent(startedBy -> b.field("StartedBy", startedBy));
        if (!build.parameters().isEmpty()) {
            build.parameters().forEach(parameter -> b.field(parameter.name(), parameter.value()));
        }
        if (build.description() != null) {
            b.field("Description", build.description());
        }
    }

    private String formatTimestamp(long timestampMillis) {
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(DATE_TIME_FORMATTER);
    }

    private String formatDuration(long durationMillis) {
        long seconds = durationMillis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public void builds(String jobId, boolean includeSuccess, boolean includeFailed, boolean includeRunning, Integer limit, boolean onlyMyBuilds) {
        JobDescriptor job = jobDescriptorProvider.get(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job " + jobId + " not found"));
        Result<String> result = commandRunner.callWithSpinner("Fetching builds...", () -> fetchBuildInfo(includeBuildStatuses(includeSuccess, includeFailed, includeRunning), limit, onlyMyBuilds, job, null));
        shellPrinter.println(result.value());
    }

    private String fetchBuildInfo(Set<Status> statuses, Integer limit, boolean onlyMyBuilds, JobDescriptor job, WorkflowJob wj) {
        return switch (job.type()) {
            case WORKFLOW -> fetchWorkflowJobBuilds(job, statuses, limit, onlyMyBuilds);
            case FREESTYLE -> fetchFreestyleJobBuilds(job, statuses, limit, onlyMyBuilds, wj != null ? wj
                : jenkinsAPI.getWorkflowJob(job.url()));
            default -> throw new IllegalArgumentException("Unsupported job type: " + job.type());
        };
    }

    private String fetchFreestyleJobBuilds(JobDescriptor job, Set<Status> statuses, Integer limit, boolean onlyMyBuilds, WorkflowJob wj) {
        List<BuildInfo> filteredBuilds = wj.builds().stream()
            .sorted(Comparator.comparingInt(WorkflowJob.Build::number).reversed())
            .map(build -> jenkinsAPI.getJobBuildInfo(job.url(), build.number()))
            .filter(buildInfo -> statuses.contains(buildInfo.result()))
            .filter(build -> !onlyMyBuilds || build.startedBy().map(userName::equals).orElse(false))
            .limit(limit)
            .toList();

        Section.Builder b = section.builder().header("Last builds");
        if (filteredBuilds.isEmpty()) {
            b.line("  No builds found.");
        } else {
            for (BuildInfo filteredBuild : filteredBuilds) {
                b.divider();
                appendBuildSummary(b, filteredBuild, filteredBuild);
            }
        }
        return b.build();
    }
}
