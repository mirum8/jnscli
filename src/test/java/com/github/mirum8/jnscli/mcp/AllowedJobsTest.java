package com.github.mirum8.jnscli.mcp;

import com.github.mirum8.jnscli.alias.AliasService;
import com.github.mirum8.jnscli.jenkins.Job;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AllowedJobsTest {

    private final AliasService aliasService = mock(AliasService.class);

    @Test
    void unrestrictedWhenCsvIsNull() {
        AllowedJobs allowedJobs = new AllowedJobs(null, aliasService);
        assertThat(allowedJobs.isUnrestricted()).isTrue();
    }

    @Test
    void unrestrictedWhenCsvIsBlank() {
        AllowedJobs allowedJobs = new AllowedJobs("   ", aliasService);
        assertThat(allowedJobs.isUnrestricted()).isTrue();
    }

    @Test
    void requireAllowedIsNoopWhenUnrestricted() {
        AllowedJobs allowedJobs = new AllowedJobs(null, aliasService);
        allowedJobs.requireAllowed("anything");
    }

    @Test
    void requireAllowedRejectsJobsOutsideAllowlist() {
        AllowedJobs allowedJobs = new AllowedJobs("job-a, job-b", aliasService);
        assertThat(allowedJobs.isUnrestricted()).isFalse();
        assertThatThrownBy(() -> allowedJobs.requireAllowed("job-c"))
            .isInstanceOf(McpToolDeniedException.class)
            .hasMessageContaining("job-c")
            .hasMessageContaining("not in the allowlist");
    }

    @Test
    void requireAllowedAcceptsAllowlistedJob() {
        AllowedJobs allowedJobs = new AllowedJobs("job-a,job-b", aliasService);
        allowedJobs.requireAllowed("job-a");
        allowedJobs.requireAllowed("job-b");
    }

    @Test
    void filterReturnsAllJobsWhenUnrestricted() {
        AllowedJobs allowedJobs = new AllowedJobs(null, aliasService);
        List<Job> jobs = List.of(
            new Job("WorkflowJob", "job-a", "https://jenkins/job/job-a/", "blue"),
            new Job("WorkflowJob", "job-b", "https://jenkins/job/job-b/", "red")
        );
        assertThat(allowedJobs.filter(jobs)).containsExactlyElementsOf(jobs);
    }

    @Test
    void filterReturnsOnlyAllowlistedJobsByName() {
        AllowedJobs allowedJobs = new AllowedJobs("job-a", aliasService);
        Job a = new Job("WorkflowJob", "job-a", "https://jenkins/job/job-a/", "blue");
        Job b = new Job("WorkflowJob", "job-b", "https://jenkins/job/job-b/", "red");
        when(aliasService.getJobUrl("job-a")).thenReturn(Optional.empty());
        assertThat(allowedJobs.filter(List.of(a, b))).containsExactly(a);
    }

    @Test
    void filterIncludesJobsMatchedByAlias() {
        AllowedJobs allowedJobs = new AllowedJobs("my-alias", aliasService);
        Job a = new Job("WorkflowJob", "real-name", "https://jenkins/job/real-name/", "blue");
        when(aliasService.getJobUrl("my-alias")).thenReturn(Optional.of("https://jenkins/job/real-name/"));
        assertThat(allowedJobs.filter(List.of(a))).containsExactly(a);
    }
}
