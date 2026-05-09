package com.github.mirum8.jnscli.mcp;

import com.github.mirum8.jnscli.JshellApplication;
import com.github.mirum8.jnscli.abort.AbortService;
import com.github.mirum8.jnscli.ai.AiService;
import com.github.mirum8.jnscli.common.JobDescriptorProvider;
import com.github.mirum8.jnscli.creds.CredentialIds;
import com.github.mirum8.jnscli.creds.CredentialType;
import com.github.mirum8.jnscli.creds.CredsFileWriter;
import com.github.mirum8.jnscli.creds.PasswordGenerator;
import com.github.mirum8.jnscli.diagnose.ErrorService;
import com.github.mirum8.jnscli.info.InfoService;
import com.github.mirum8.jnscli.jenkins.CredentialsAPI;
import com.github.mirum8.jnscli.jenkins.JenkinsAPI;
import com.github.mirum8.jnscli.jenkins.QueueItemLocation;
import com.github.mirum8.jnscli.jenkins.WorkflowJob;
import com.github.mirum8.jnscli.list.ListService;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.pipeline.PipelineCreateService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mirum8.jnscli.jenkins.JenkinsApiUtils;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Profile(JshellApplication.MCP_PROFILE)
public class McpTools {

    private static final String DEFAULT_SCOPE = "GLOBAL";

    private final ListService listService;
    private final InfoService infoService;
    private final ErrorService errorService;
    private final AbortService abortService;
    private final AiService aiService;
    private final JenkinsAPI jenkinsAPI;
    private final JobDescriptorProvider jobDescriptorProvider;
    private final AllowedJobs allowedJobs;
    private final McpJsonCapture capture;
    private final CredentialsAPI credentialsAPI;
    private final PasswordGenerator passwordGenerator;
    private final CredsFileWriter credsFileWriter;
    private final PipelineCreateService pipelineCreateService;
    private final ObjectMapper mapper = JenkinsApiUtils.createObjectMapper();

    public McpTools(ListService listService,
                    InfoService infoService,
                    ErrorService errorService,
                    AbortService abortService,
                    AiService aiService,
                    JenkinsAPI jenkinsAPI,
                    JobDescriptorProvider jobDescriptorProvider,
                    AllowedJobs allowedJobs,
                    McpJsonCapture capture,
                    CredentialsAPI credentialsAPI,
                    PasswordGenerator passwordGenerator,
                    CredsFileWriter credsFileWriter,
                    PipelineCreateService pipelineCreateService) {
        this.listService = listService;
        this.infoService = infoService;
        this.errorService = errorService;
        this.abortService = abortService;
        this.aiService = aiService;
        this.jenkinsAPI = jenkinsAPI;
        this.jobDescriptorProvider = jobDescriptorProvider;
        this.allowedJobs = allowedJobs;
        this.capture = capture;
        this.credentialsAPI = credentialsAPI;
        this.passwordGenerator = passwordGenerator;
        this.credsFileWriter = credsFileWriter;
        this.pipelineCreateService = pipelineCreateService;
    }

    public record TriggerBuildResult(String job, int buildNumber, String url, String queueLocation) {
    }

    public record CredentialCreated(String id, String type, String filePath) {
    }

    public record CreatePipelineResult(String name, String url, String repo, String branch, String scriptPath, String folder) {
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

    @Tool(name = "list_credentials", description = "List Jenkins credentials in the global system store. Returns id, typeName, and description for each. Secrets are never returned.")
    public String listCredentials() {
        try {
            return mapper.writeValueAsString(credentialsAPI.list());
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize credentials", e);
        }
    }

    @Tool(name = "create_user_pass_credential", description = "Create a Jenkins username/password credential in the global system store. The password is never returned in the response. When `random=true`, a strong password is generated and written to a file under ~/.config/jns/creds/; the returned filePath is the only way to retrieve it. When `password` is supplied, no file is written and filePath is null. Exactly one of `password` or `random=true` must be provided.")
    public CredentialCreated createUserPassCredential(@ToolParam(description = "Credential id.") String id,
                                                      @ToolParam(description = "Username.") String username,
                                                      @ToolParam(required = false, description = "Password. Mutually exclusive with random=true.") String password,
                                                      @ToolParam(required = false, description = "If true, generate a random password and write it to a file. Mutually exclusive with password.") Boolean random,
                                                      @ToolParam(required = false, description = "Optional description.") String description,
                                                      @ToolParam(required = false, description = "Credential scope. GLOBAL (default) or SYSTEM.") String scope) {
        CredentialIds.validate(id);
        boolean useRandom = Boolean.TRUE.equals(random);
        if (useRandom && password != null) {
            throw new IllegalArgumentException("`password` and `random=true` are mutually exclusive");
        }
        if (!useRandom && password == null) {
            throw new IllegalArgumentException("Provide `password` or set `random=true`");
        }
        String effectiveScope = scope == null || scope.isBlank() ? DEFAULT_SCOPE : scope;
        String effectivePassword = useRandom ? passwordGenerator.generate() : password;
        credentialsAPI.createUserPass(id, username, effectivePassword, description, effectiveScope);
        String filePath = null;
        if (useRandom) {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("username", username);
            fields.put("password", effectivePassword);
            Path written = credsFileWriter.write(id, CredentialType.USER_PASS, fields);
            filePath = written.toString();
        }
        return new CredentialCreated(id, CredentialType.USER_PASS.displayName(), filePath);
    }

    @Tool(name = "create_secret_text_credential", description = "Create a Jenkins secret-text credential in the global system store. The secret is never returned in the response. When `random=true`, a strong secret is generated and written to a file under ~/.config/jns/creds/; the returned filePath is the only way to retrieve it. When `secret` is supplied, no file is written and filePath is null. Exactly one of `secret` or `random=true` must be provided.")
    public CredentialCreated createSecretTextCredential(@ToolParam(description = "Credential id.") String id,
                                                        @ToolParam(required = false, description = "Secret value. Mutually exclusive with random=true.") String secret,
                                                        @ToolParam(required = false, description = "If true, generate a random secret and write it to a file. Mutually exclusive with secret.") Boolean random,
                                                        @ToolParam(required = false, description = "Optional description.") String description,
                                                        @ToolParam(required = false, description = "Credential scope. GLOBAL (default) or SYSTEM.") String scope) {
        CredentialIds.validate(id);
        boolean useRandom = Boolean.TRUE.equals(random);
        if (useRandom && secret != null) {
            throw new IllegalArgumentException("`secret` and `random=true` are mutually exclusive");
        }
        if (!useRandom && secret == null) {
            throw new IllegalArgumentException("Provide `secret` or set `random=true`");
        }
        String effectiveScope = scope == null || scope.isBlank() ? DEFAULT_SCOPE : scope;
        String effectiveSecret = useRandom ? passwordGenerator.generate() : secret;
        credentialsAPI.createSecretText(id, effectiveSecret, description, effectiveScope);
        String filePath = null;
        if (useRandom) {
            Map<String, String> fields = new LinkedHashMap<>();
            fields.put("secret", effectiveSecret);
            Path written = credsFileWriter.write(id, CredentialType.SECRET_TEXT, fields);
            filePath = written.toString();
        }
        return new CredentialCreated(id, CredentialType.SECRET_TEXT.displayName(), filePath);
    }

    @Tool(name = "delete_credential", description = "Delete a Jenkins credential from the global system store by id.")
    public String deleteCredential(@ToolParam(description = "Credential id.") String id) {
        CredentialIds.validate(id);
        credentialsAPI.delete(id);
        return "Deleted credential '" + id + "'";
    }

    @Tool(name = "create_pipeline", description = "Create a Jenkins Pipeline-from-SCM job that checks out a Jenkinsfile from a Git repository.")
    public CreatePipelineResult createPipeline(
            @ToolParam(description = "Pipeline job name.") String name,
            @ToolParam(description = "Git repository URL.") String repo,
            @ToolParam(required = false, description = "Branch (default: main).") String branch,
            @ToolParam(required = false, description = "Path to the Jenkinsfile in the repo (default: Jenkinsfile).") String scriptPath,
            @ToolParam(required = false, description = "Optional folder path to create the pipeline in.") String folder,
            @ToolParam(required = false, description = "Optional Jenkins credentialsId for SCM checkout.") String credentialsId,
            @ToolParam(required = false, description = "Optional job description.") String description) {
        allowedJobs.requireAllowed(name);
        PipelineCreateService.CreatePipelineJson result = pipelineCreateService.createForMcp(
            name, repo, branch, scriptPath, folder, credentialsId, description);
        return new CreatePipelineResult(result.name(), result.url(), result.repo(),
            result.branch(), result.scriptPath(), result.folder());
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
