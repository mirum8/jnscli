package com.github.mirum8.jnscli.mcp;

import com.github.mirum8.jnscli.JshellApplication;
import com.github.mirum8.jnscli.abort.AbortService;
import com.github.mirum8.jnscli.ai.AiService;
import com.github.mirum8.jnscli.common.JobDescriptorProvider;
import com.github.mirum8.jnscli.diagnose.ErrorService;
import com.github.mirum8.jnscli.info.InfoService;
import com.github.mirum8.jnscli.jenkins.JenkinsAPI;
import com.github.mirum8.jnscli.jenkins.QueueItemLocation;
import com.github.mirum8.jnscli.jenkins.WorkflowJob;
import com.github.mirum8.jnscli.list.ListService;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mirum8.jnscli.jenkins.JenkinsApiUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Profile(JshellApplication.MCP_PROFILE)
public class McpTools {

    private final ListService listService;
    private final InfoService infoService;
    private final ErrorService errorService;
    private final AbortService abortService;
    private final AiService aiService;
    private final JenkinsAPI jenkinsAPI;
    private final JobDescriptorProvider jobDescriptorProvider;
    private final AllowedJobs allowedJobs;
    private final McpJsonCapture capture;
    private final ObjectMapper mapper = JenkinsApiUtils.createObjectMapper();

    public McpTools(ListService listService,
                    InfoService infoService,
                    ErrorService errorService,
                    AbortService abortService,
                    AiService aiService,
                    JenkinsAPI jenkinsAPI,
                    JobDescriptorProvider jobDescriptorProvider,
                    AllowedJobs allowedJobs,
                    McpJsonCapture capture) {
        this.listService = listService;
        this.infoService = infoService;
        this.errorService = errorService;
        this.abortService = abortService;
        this.aiService = aiService;
        this.jenkinsAPI = jenkinsAPI;
        this.jobDescriptorProvider = jobDescriptorProvider;
        this.allowedJobs = allowedJobs;
        this.capture = capture;
    }

    public record TriggerBuildResult(String job, int buildNumber, String url, String queueLocation) {
    }

    @Tool(name = "list_jobs", description = "List Jenkins jobs visible to this MCP server. If an allowlist was provided at startup, only those jobs are returned.")
    public String listJobs(@ToolParam(required = false, description = "Optional folder name or numeric ID prefixed with %. When omitted, lists top-level jobs.") String folder) {
        capture.clear();
        if (folder == null || folder.isBlank()) {
            listService.listJobs();
        } else {
            listService.listJobs(folder);
        }
        String json = capture.get();
        return filterJobsJson(json);
    }

    @Tool(name = "get_job_info", description = "Get details and recent builds for a Jenkins job.")
    public String getJobInfo(@ToolParam(description = "Job name or alias.") String jobName,
                             @ToolParam(required = false, description = "Specific build number to inspect; latest builds if omitted.") Integer buildNumber,
                             @ToolParam(required = false, description = "Maximum number of builds to include. Defaults to 10.") Integer limit) {
        allowedJobs.requireAllowed(jobName);
        capture.clear();
        int effectiveLimit = limit == null ? 10 : limit;
        infoService.info(jobName, buildNumber, false, false, false, effectiveLimit, false);
        return capture.get();
    }

    @Tool(name = "trigger_build", description = "Trigger a Jenkins build. Returns the next build number; does not wait for completion.")
    public TriggerBuildResult triggerBuild(@ToolParam(description = "Job name or alias.") String jobName,
                                           @ToolParam(required = false, description = "Build parameters as a name->value map. Omit for parameter-less jobs.") Map<String, String> parameters) {
        allowedJobs.requireAllowed(jobName);
        JobDescriptor job = jobDescriptorProvider.get(jobName)
            .orElseThrow(() -> new IllegalArgumentException("Job " + jobName + " not found"));
        WorkflowJob workflowJob = jenkinsAPI.getWorkflowJob(job.url());
        if (!workflowJob.buildable()) {
            throw new IllegalArgumentException("Job " + jobName + " is not buildable");
        }
        int nextBuildNumber = workflowJob.nextBuildNumber();
        QueueItemLocation queueItem;
        if (parameters == null || parameters.isEmpty()) {
            queueItem = jenkinsAPI.runJob(job.url());
        } else {
            List<String> paramList = parameters.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .toList();
            queueItem = jenkinsAPI.runJob(job.url(), paramList);
        }
        return new TriggerBuildResult(job.name(), nextBuildNumber,
            job.url() + "/" + nextBuildNumber,
            queueItem == null ? null : queueItem.url());
    }

    @Tool(name = "abort_build", description = "Abort a running Jenkins build. If buildNumber is omitted, the latest running build is aborted.")
    public String abortBuild(@ToolParam(description = "Job name or alias.") String jobName,
                             @ToolParam(required = false, description = "Specific build number to abort. Latest running build if omitted.") Integer buildNumber) {
        allowedJobs.requireAllowed(jobName);
        capture.clear();
        if (buildNumber == null) {
            abortService.abort(jobName);
        } else {
            abortService.abort(jobName, buildNumber);
        }
        return capture.get();
    }

    @Tool(name = "get_build_errors", description = "Fetch the failing-stage log for a Jenkins build.")
    public String getBuildErrors(@ToolParam(description = "Job name or alias.") String jobName,
                                 @ToolParam(required = false, description = "Specific build number; latest failed build if omitted.") Integer buildNumber) {
        allowedJobs.requireAllowed(jobName);
        capture.clear();
        errorService.getError(jobName, buildNumber, false, false);
        return capture.get();
    }

    @Tool(name = "analyze_build_with_ai", description = "Run the configured AI provider against a build's failure log and return a natural-language analysis.")
    public String analyzeBuildWithAi(@ToolParam(description = "Job name or alias.") String jobName,
                                     @ToolParam(required = false, description = "Specific build number; latest failed build if omitted.") Integer buildNumber) {
        allowedJobs.requireAllowed(jobName);
        JobDescriptor job = jobDescriptorProvider.get(jobName)
            .orElseThrow(() -> new IllegalArgumentException("Job " + jobName + " not found"));
        int target;
        if (buildNumber != null) {
            target = buildNumber;
        } else {
            WorkflowJob.Build lastBuild = jenkinsAPI.getWorkflowJob(job.url()).lastBuild();
            if (lastBuild == null) {
                return "No builds found for " + jobName;
            }
            target = lastBuild.number();
        }
        String log = errorService.getErrors(job, target);
        if (log == null || log.isEmpty()) {
            return "No errors found for build " + target;
        }
        return aiService.analyzeLog(log);
    }

    private String filterJobsJson(String json) {
        if (json == null || allowedJobs.isUnrestricted()) {
            return json;
        }
        try {
            List<ListService.JobJson> rows = mapper.readValue(json,
                mapper.getTypeFactory().constructCollectionType(List.class, ListService.JobJson.class));
            List<ListService.JobJson> filtered = rows.stream()
                .filter(r -> allowedJobs.allowed().contains(r.name()))
                .toList();
            return mapper.writeValueAsString(filtered);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to filter jobs JSON", e);
        }
    }
}
