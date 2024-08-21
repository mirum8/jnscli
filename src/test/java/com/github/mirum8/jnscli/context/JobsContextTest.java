package com.github.mirum8.jnscli.context;

import com.github.mirum8.jnscli.jenkins.Job;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.settings.SettingsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JobsContextTest {

    @TempDir
    Path tempDir;

    private SettingsProperties settingsProperties;
    private JobsContext jobsContext;

    @BeforeEach
    void setUp() {
        settingsProperties = mock(SettingsProperties.class);
        when(settingsProperties.directory()).thenReturn(tempDir.toString());
        jobsContext = new JobsContext(settingsProperties);
    }

    @Test
    void refreshJobIds_createsFileIfNotExists() {
        List<Job> jobs = List.of(
            new Job("hudson.model.FreeStyleProject", "job1", "http://example.com/job1", "blue"),
            new Job("org.jenkinsci.plugins.workflow.job.WorkflowJob", "job2", "http://example.com/job2", "red")
        );

        jobsContext.refreshJobIds(jobs, false);

        assertTrue(Files.exists(tempDir.resolve(JobsContext.MAPPING_FILENAME)));
    }

    @Test
    void refreshJobIds_writesJobsToFile() throws IOException {
        List<Job> jobs = List.of(
            new Job("hudson.model.FreeStyleProject", "job1", "http://example.com/job1", "blue"),
            new Job("org.jenkinsci.plugins.workflow.job.WorkflowJob", "job2", "http://example.com/job2", "red")
        );

        jobsContext.refreshJobIds(jobs, false);

        List<String> lines = Files.readAllLines(tempDir.resolve(JobsContext.MAPPING_FILENAME));
        assertEquals(2, lines.size());

        String[] parts1 = lines.get(0).split(";");
        assertEquals("1", parts1[0]);
        assertEquals("job1", parts1[1]);
        assertEquals("http://example.com/job1", parts1[2]);
        assertEquals(JobType.FREESTYLE.name(), parts1[3]);

        String[] parts2 = lines.get(1).split(";");
        assertEquals("2", parts2[0]);
        assertEquals("job2", parts2[1]);
        assertEquals("http://example.com/job2", parts2[2]);
        assertEquals(JobType.WORKFLOW.name(), parts2[3]);
    }

    @Test
    void findJobById_returnsCorrectJob() throws IOException {
        List<String> lines = List.of(
            "1;job1;http://example.com/job1;FREESTYLE",
            "2;job2;http://example.com/job2;WORKFLOW"
        );
        Files.write(tempDir.resolve(JobsContext.MAPPING_FILENAME), lines);

        Optional<JobDescriptor> job1 = jobsContext.findJobById(1);
        assertTrue(job1.isPresent());
        assertEquals("job1", job1.get().name());
        assertEquals("http://example.com/job1", job1.get().url());
        assertEquals(JobType.FREESTYLE, job1.get().type());

        Optional<JobDescriptor> job2 = jobsContext.findJobById(2);
        assertTrue(job2.isPresent());
        assertEquals("job2", job2.get().name());
        assertEquals("http://example.com/job2", job2.get().url());
        assertEquals(JobType.WORKFLOW, job2.get().type());
    }

    @Test
    void findJobByName_returnsCorrectJob() throws IOException {
        List<String> lines = List.of(
            "1;job1;http://example.com/job1;FREESTYLE",
            "2;job2;http://example.com/job2;WORKFLOW"
        );
        Files.write(tempDir.resolve(JobsContext.MAPPING_FILENAME), lines);

        Optional<JobDescriptor> job1 = jobsContext.findJobByName("job1");
        assertTrue(job1.isPresent());
        assertEquals(1, job1.get().id());
        assertEquals("http://example.com/job1", job1.get().url());
        assertEquals(JobType.FREESTYLE, job1.get().type());

        Optional<JobDescriptor> job2 = jobsContext.findJobByName("job2");
        assertTrue(job2.isPresent());
        assertEquals(2, job2.get().id());
        assertEquals("http://example.com/job2", job2.get().url());
        assertEquals(JobType.WORKFLOW, job2.get().type());
    }
}
