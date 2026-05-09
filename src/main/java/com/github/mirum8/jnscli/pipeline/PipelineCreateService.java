package com.github.mirum8.jnscli.pipeline;

import com.github.mirum8.jnscli.jenkins.JenkinsAPI;
import com.github.mirum8.jnscli.jenkins.JenkinsApiUtils;
import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.settings.SettingsService;
import com.github.mirum8.jnscli.shell.JsonOutput;
import com.github.mirum8.jnscli.shell.Messages;
import com.github.mirum8.jnscli.shell.OutputContext;
import com.github.mirum8.jnscli.shell.Section;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;

@Service
public class PipelineCreateService {

    static final String DEFAULT_BRANCH = "main";
    static final String DEFAULT_SCRIPT_PATH = "Jenkinsfile";

    private final JenkinsAPI jenkinsAPI;
    private final SettingsService settingsService;
    private final CommandRunner commandRunner;
    private final ShellPrinter shellPrinter;
    private final ShellPrompter shellPrompter;
    private final Messages messages;
    private final Section section;
    private final OutputContext outputContext;
    private final JsonOutput jsonOutput;

    PipelineCreateService(JenkinsAPI jenkinsAPI,
                          SettingsService settingsService,
                          CommandRunner commandRunner,
                          ShellPrinter shellPrinter,
                          ShellPrompter shellPrompter,
                          Messages messages,
                          Section section,
                          OutputContext outputContext,
                          JsonOutput jsonOutput) {
        this.jenkinsAPI = jenkinsAPI;
        this.settingsService = settingsService;
        this.commandRunner = commandRunner;
        this.shellPrinter = shellPrinter;
        this.shellPrompter = shellPrompter;
        this.messages = messages;
        this.section = section;
        this.outputContext = outputContext;
        this.jsonOutput = jsonOutput;
    }

    public record CreatePipelineJson(String name, String url, String repo, String branch, String scriptPath, String folder) {
    }

    public void create(String name, String repo, String branch, String scriptPath, String folder, String credentialsId, String description) {
        if (outputContext.isJson()) {
            CreatePipelineJson result = createInternal(requireNonBlank(name, "name"),
                requireNonBlank(repo, "repo"), branch, scriptPath, folder, credentialsId, description);
            jsonOutput.println(result);
            return;
        }

        String resolvedName = (name == null || name.isBlank())
            ? shellPrompter.promptString("Pipeline name", null)
            : name;
        String resolvedRepo = (repo == null || repo.isBlank())
            ? shellPrompter.promptString("Git repository URL", null)
            : repo;

        requireNonBlank(resolvedName, "name");
        requireNonBlank(resolvedRepo, "repo");

        CreatePipelineJson value = commandRunner.callWithSpinner(
            "Creating pipeline " + resolvedName,
            () -> createInternal(resolvedName, resolvedRepo, branch, scriptPath, folder, credentialsId, description))
            .value();

        messages.success("Pipeline " + value.name() + " created");
        shellPrinter.println(section.builder()
            .field("URL", value.url())
            .field("Repo", value.repo())
            .field("Branch", value.branch())
            .field("Script", value.scriptPath())
            .build());
    }

    public CreatePipelineJson createForMcp(String name, String repo, String branch, String scriptPath,
                                           String folder, String credentialsId, String description) {
        return createInternal(requireNonBlank(name, "name"),
            requireNonBlank(repo, "repo"), branch, scriptPath, folder, credentialsId, description);
    }

    private CreatePipelineJson createInternal(String name, String repo, String branch, String scriptPath,
                                              String folder, String credentialsId, String description) {
        String effectiveBranch = (branch == null || branch.isBlank()) ? DEFAULT_BRANCH : branch;
        String effectiveScriptPath = (scriptPath == null || scriptPath.isBlank()) ? DEFAULT_SCRIPT_PATH : scriptPath;
        String folderUrl = resolveFolderUrl(folder);
        String xml = buildFlowDefinitionXml(repo, effectiveBranch, effectiveScriptPath, credentialsId, description);
        String jobUrl = jenkinsAPI.createPipelineJob(folderUrl, name, xml);
        return new CreatePipelineJson(name, jobUrl, repo, effectiveBranch, effectiveScriptPath, folder);
    }

    String resolveFolderUrl(String folder) {
        if (folder == null || folder.isBlank()) {
            return null;
        }
        String server = settingsService.readSettings().server();
        if (folder.startsWith("http://") || folder.startsWith("https://")) {
            requireSameOrigin(server, folder);
            return folder;
        }
        String trimmed = folder.replaceAll("^/+", "").replaceAll("/+$", "");
        StringBuilder path = new StringBuilder();
        for (String segment : trimmed.split("/+")) {
            if (segment.isEmpty()) {
                continue;
            }
            path.append("/job/").append(JenkinsApiUtils.encodePathSegment(segment));
        }
        return (server.endsWith("/") ? server.substring(0, server.length() - 1) : server) + path;
    }

    private static void requireSameOrigin(String server, String candidate) {
        URI serverUri = parseUri(server, "configured server");
        URI candidateUri = parseUri(candidate, "folder");
        boolean sameOrigin = Objects.equals(serverUri.getScheme(), candidateUri.getScheme())
            && Objects.equals(serverUri.getHost(), candidateUri.getHost())
            && serverUri.getPort() == candidateUri.getPort();
        if (!sameOrigin) {
            throw new IllegalArgumentException("folder URL " + candidate + " does not match configured Jenkins server " + server);
        }
    }

    private static URI parseUri(String value, String label) {
        try {
            return new URI(value);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid " + label + " URL: " + value, e);
        }
    }

    static String buildFlowDefinitionXml(String repo, String branch, String scriptPath, String credentialsId, String description) {
        StringBuilder sb = new StringBuilder(512);
        sb.append("<?xml version='1.1' encoding='UTF-8'?>\n");
        sb.append("<flow-definition plugin=\"workflow-job\">\n");
        sb.append("  <description>").append(escape(description)).append("</description>\n");
        sb.append("  <keepDependencies>false</keepDependencies>\n");
        sb.append("  <definition class=\"org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition\" plugin=\"workflow-cps\">\n");
        sb.append("    <scm class=\"hudson.plugins.git.GitSCM\" plugin=\"git\">\n");
        sb.append("      <configVersion>2</configVersion>\n");
        sb.append("      <userRemoteConfigs>\n");
        sb.append("        <hudson.plugins.git.UserRemoteConfig>\n");
        sb.append("          <url>").append(escape(repo)).append("</url>\n");
        if (credentialsId != null && !credentialsId.isBlank()) {
            sb.append("          <credentialsId>").append(escape(credentialsId)).append("</credentialsId>\n");
        }
        sb.append("        </hudson.plugins.git.UserRemoteConfig>\n");
        sb.append("      </userRemoteConfigs>\n");
        sb.append("      <branches>\n");
        sb.append("        <hudson.plugins.git.BranchSpec>\n");
        sb.append("          <name>*/").append(escape(branch)).append("</name>\n");
        sb.append("        </hudson.plugins.git.BranchSpec>\n");
        sb.append("      </branches>\n");
        sb.append("      <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>\n");
        sb.append("      <submoduleCfg class=\"empty-list\"/>\n");
        sb.append("      <extensions/>\n");
        sb.append("    </scm>\n");
        sb.append("    <scriptPath>").append(escape(scriptPath)).append("</scriptPath>\n");
        sb.append("    <lightweight>true</lightweight>\n");
        sb.append("  </definition>\n");
        sb.append("  <triggers/>\n");
        sb.append("  <disabled>false</disabled>\n");
        sb.append("</flow-definition>\n");
        return sb.toString();
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '&' -> out.append("&amp;");
                case '<' -> out.append("&lt;");
                case '>' -> out.append("&gt;");
                case '"' -> out.append("&quot;");
                case '\'' -> out.append("&apos;");
                default -> out.append(c);
            }
        }
        return out.toString();
    }

    private static String requireNonBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}
