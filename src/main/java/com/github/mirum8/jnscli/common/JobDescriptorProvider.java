package com.github.mirum8.jnscli.common;

import com.github.mirum8.jnscli.alias.AliasService;
import com.github.mirum8.jnscli.context.JobType;
import com.github.mirum8.jnscli.context.JobsContext;
import com.github.mirum8.jnscli.jenkins.JenkinsAdapter;
import com.github.mirum8.jnscli.model.JobDescriptor;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.github.mirum8.jnscli.util.Strings.isJobNumber;

@Component
public class JobDescriptorProvider {
    private final AliasService aliasService;
    private final JenkinsAdapter jenkinsAdapter;
    private final JobsContext jobsContext;

    public JobDescriptorProvider(AliasService aliasService, JenkinsAdapter jenkinsAdapter, JobsContext jobsContext) {
        this.aliasService = aliasService;
        this.jenkinsAdapter = jenkinsAdapter;
        this.jobsContext = jobsContext;
    }

    public Optional<JobDescriptor> get(String jobId) {
        return isJobNumber(jobId) ? getJobDescriptorUrlById(jobId) : getJobDescriptorByName(jobId);
    }

    private Optional<JobDescriptor> getJobDescriptorByName(String jobName) {
        return aliasService.getJobUrl(jobName)
            .map(url -> JobDescriptor.builder().name(jobName).url(url).alias(jobName).build())
            .or(() -> jobsContext.findJobByName(jobName))
            .or(() -> jenkinsAdapter.getJobs()
                .stream().filter(job -> job.name().equals(jobName))
                .findFirst().map(job -> JobDescriptor.builder().name(job.name()).url(job.url()).type(JobType.fromName(job.aClass())).build()));
    }

    private Optional<JobDescriptor> getJobDescriptorUrlById(String jobId) {
        return jobsContext.findJobById(Integer.parseInt(jobId.substring(1)));
    }
}
