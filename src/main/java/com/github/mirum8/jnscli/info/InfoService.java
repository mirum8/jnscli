package com.github.mirum8.jnscli.info;

import com.github.mirum8.jnscli.common.JobDescriptorProvider;
import com.github.mirum8.jnscli.jenkins.*;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.settings.SettingsService;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.shell.TextColor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import static com.github.mirum8.jnscli.shell.TextFormatter.colored;

@Component
public class InfoService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JenkinsAdapter jenkinsAdapter;
    private final ShellPrinter shellPrinter;
    private final JobDescriptorProvider jobDescriptorProvider;
    private final String userName;

    public InfoService(JenkinsAdapter jenkinsAdapter, ShellPrinter shellPrinter, JobDescriptorProvider jobDescriptorProvider, SettingsService settingsService) {
        this.jenkinsAdapter = jenkinsAdapter;
        this.shellPrinter = shellPrinter;
        this.jobDescriptorProvider = jobDescriptorProvider;
        this.userName = settingsService.readSettings().username();
    }

    public void info(String jobId,
                     Integer buildNumber,
                     boolean includeSuccess,
                     boolean includeFailed,
                     boolean includeRunning, Integer limit, boolean buildsRunByMe) {
        JobDescriptor job = jobDescriptorProvider.get(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job " + jobId + " not found"));

        if (buildNumber != null) {
            printFullBuildInfo(job.url(), buildNumber);
        } else {
            printJobInfo(job, includeBuildStatuses(includeSuccess, includeFailed, includeRunning), limit, buildsRunByMe);
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

    private void printJobInfo(JobDescriptor job, Set<Status> statuses, Integer limit, boolean buildsRunByMe) {
        WorkflowJob wj = jenkinsAdapter.getWorkflowJob(job.url());
        printGeneralJobInfo(job, wj);
        printBuildInfo(statuses, limit, buildsRunByMe, job, wj);
    }

    private void printWorkflowJobBuilds(JobDescriptor job, Set<Status> statuses, int limit, boolean buildsRunByMe) {
        StringBuilder sb = new StringBuilder();
        sb.append(colored("Last builds:\n", TextColor.CYAN));

        record RunWithBuildInfo(Run run, BuildInfo buildInfo) {
        }

        List<Run> builds = jenkinsAdapter.getJobRuns(job.url());
        List<RunWithBuildInfo> filteredBuilds = builds.stream()
            .filter(build -> statuses.contains(build.status()))
            .map(run -> new RunWithBuildInfo(run, jenkinsAdapter.getJobBuildInfo(job.url(), run.id())))
            .sorted(Comparator.<RunWithBuildInfo>comparingInt(r -> r.buildInfo.number()).reversed())
            .filter(r -> !buildsRunByMe || r.buildInfo().startedBy().isPresent() && r.buildInfo().startedBy().get().equals(userName))
            .limit(limit)
            .toList();

        if (!filteredBuilds.isEmpty()) {
            for (RunWithBuildInfo filteredBuild : filteredBuilds) {
                sb.append("----------------------------------------\n");
                sb.append(getBuildSummary(filteredBuild.run(), filteredBuild.buildInfo()));
            }
        } else {
            sb.append("  No builds found.\n");
        }

        shellPrinter.println(sb.toString());
    }

    private void printGeneralJobInfo(JobDescriptor job, WorkflowJob wj) {
        StringBuilder sb = new StringBuilder();
        sb.append(colored("Job Information:\n", TextColor.YELLOW));
        sb.append(colored("  Name: ", TextColor.CYAN)).append(wj.name()).append("\n");
        sb.append(colored("  URL:  ", TextColor.CYAN)).append(wj.url()).append("\n");
        if (job.alias() != null) {
            sb.append(colored("  Alias: ", TextColor.CYAN)).append(job.alias()).append("\n");
        }
        if (wj.description() != null && !wj.description().trim().isEmpty()) {
            sb.append(colored("  Description: ", TextColor.CYAN)).append(wj.description()).append("\n");
        }
        if (wj.property() != null && !wj.property().isEmpty()) {
            sb.append(colored("  Parameters:\n", TextColor.CYAN));
            wj.property().stream().map(WorkflowJob.Property::parameterDefinitions).filter(Objects::nonNull)
                .flatMap(List::stream)
                .forEach(parameter -> sb.append("    ").append(colored(parameter.name() + ": ", TextColor.CYAN)).append(parameter.defaultValue()).append("\n"));
        }
        shellPrinter.println(sb.toString());
    }

    private void printFullBuildInfo(String jobUrl, int buildNumber) {
        WorkflowRun wr = jenkinsAdapter.getJobBuildDescription(jobUrl, buildNumber);
        StringBuilder sb = new StringBuilder();
        sb.append(getBuildSummary(wr, jenkinsAdapter.getJobBuildInfo(jobUrl, wr.id())));
        if (!wr.stages().isEmpty()) {
            sb.append(colored("\n  Stages:", TextColor.CYAN)).append("\n");
            wr.stages().forEach(stage -> sb.append(formatStageInfo(stage)));
        }
        shellPrinter.println(sb.toString());
    }

    private String getBuildSummary(Build run, BuildInfo build) {
        var sb = new StringBuilder();
        sb.append(colored("Build " + build.displayName(), TextColor.YELLOW)).append("\n")
            .append(colored("  Status:    ", TextColor.CYAN)).append(getColoredStatus(run.status())).append("\n")
            .append(colored("  StartedAt: ", TextColor.CYAN)).append(formatTimestamp(build.timestamp())).append("\n")
            .append(colored("  Duration:  ", TextColor.CYAN)).append(formatDuration(build.duration())).append("\n");
        build.startedBy().ifPresent(startedBy ->
            sb.append(colored("  StartedBy: ", TextColor.CYAN)).append(startedBy).append("\n"));
        if (!build.parameters().isEmpty()) {
            build.parameters().forEach(parameter ->
                sb.append(colored("  " + parameter.name() + ": ", TextColor.CYAN)).append(parameter.value()).append("\n"));
        }
        if (build.description() != null) {
            sb.append(colored("  Description: ", TextColor.CYAN)).append(build.description()).append("\n");
        }
        return sb.toString();
    }

    private String formatStageInfo(WorkflowRun.Stage stage) {
        return "   " + stage.name() + ": " + getColoredStatus(Status.valueOf(stage.status().toUpperCase())) + "\n";
    }

    private String getColoredStatus(Status status) {
        return switch (status) {
            case SUCCESS -> colored(status.toString(), TextColor.GREEN);
            case FAILED, FAILURE -> colored(status.toString(), TextColor.RED);
            case ABORTED, IN_PROGRESS -> colored(status.toString(), TextColor.YELLOW);
            default -> status.name();
        };
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

    public void builds(String jobId, boolean includeSuccess, boolean includeFailed, boolean includeRunning, Integer limit, boolean buildsRunByMe) {
        JobDescriptor job = jobDescriptorProvider.get(jobId)
            .orElseThrow(() -> new IllegalArgumentException("Job " + jobId + " not found"));
        printBuildInfo(includeBuildStatuses(includeSuccess, includeFailed, includeRunning), limit, buildsRunByMe, job, null);
    }

    private void printBuildInfo(Set<Status> statuses, Integer limit, boolean buildsRunByMe, JobDescriptor job, WorkflowJob wj) {
        switch (job.type()) {
            case WORKFLOW -> printWorkflowJobBuilds(job, statuses, limit, buildsRunByMe);
            case FREESTYLE ->
                printFreestyleJobBuilds(job, statuses, limit, buildsRunByMe, wj != null ? wj : jenkinsAdapter.getWorkflowJob(job.url()));
            default -> throw new IllegalArgumentException("Unsupported job type: " + job.type());
        }
    }

    private void printFreestyleJobBuilds(JobDescriptor job, Set<Status> statuses, Integer limit, boolean buildsRunByMe, WorkflowJob wj) {
        StringBuilder sb = new StringBuilder();
        sb.append(colored("Last builds:\n", TextColor.CYAN));

        List<BuildInfo> filteredBuilds = wj.builds().stream()
            .sorted(Comparator.comparingInt(WorkflowJob.Build::number).reversed())
            .map(build -> jenkinsAdapter.getJobBuildInfo(job.url(), build.number()))
            .filter(buildInfo -> statuses.contains(buildInfo.result()))
            .filter(build -> !buildsRunByMe || build.startedBy().isPresent() && build.startedBy().get().equals(userName))
            .limit(limit)
            .toList();

        if (!filteredBuilds.isEmpty()) {
            for (BuildInfo filteredBuild : filteredBuilds) {
                sb.append("----------------------------------------\n");
                sb.append(getBuildSummary(filteredBuild, jenkinsAdapter.getJobBuildInfo(job.url(), filteredBuild.number())));
            }
        } else {
            sb.append("  No builds found.\n");
        }

        shellPrinter.println(sb.toString());
    }
}
