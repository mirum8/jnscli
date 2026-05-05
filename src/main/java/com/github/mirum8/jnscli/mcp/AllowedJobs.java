package com.github.mirum8.jnscli.mcp;

import com.github.mirum8.jnscli.alias.AliasService;
import com.github.mirum8.jnscli.jenkins.Job;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class AllowedJobs {

    private final Set<String> allowed;
    private final AliasService aliasService;

    public AllowedJobs(String csv, AliasService aliasService) {
        this.aliasService = aliasService;
        this.allowed = parseCsv(csv);
    }

    private static Set<String> parseCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isUnrestricted() {
        return allowed.isEmpty();
    }

    public Set<String> allowed() {
        return allowed;
    }

    public void requireAllowed(String jobName) {
        if (isUnrestricted()) {
            return;
        }
        if (jobName == null || !allowed.contains(jobName)) {
            throw new McpToolDeniedException("Job '" + jobName + "' is not in the allowlist");
        }
    }

    public List<Job> filter(List<Job> jobs) {
        if (isUnrestricted()) {
            return jobs;
        }
        return jobs.stream().filter(this::isJobAllowed).toList();
    }

    private boolean isJobAllowed(Job job) {
        if (allowed.contains(job.name())) {
            return true;
        }
        return allowed.stream()
            .anyMatch(name -> aliasService.getJobUrl(name).map(url -> url.equals(job.url())).orElse(false));
    }
}
