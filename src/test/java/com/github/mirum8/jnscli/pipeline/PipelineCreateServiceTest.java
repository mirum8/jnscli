package com.github.mirum8.jnscli.pipeline;

import com.github.mirum8.jnscli.jenkins.JenkinsAPI;
import com.github.mirum8.jnscli.runner.CommandRunner;
import com.github.mirum8.jnscli.runner.Result;
import com.github.mirum8.jnscli.settings.Settings;
import com.github.mirum8.jnscli.settings.SettingsService;
import com.github.mirum8.jnscli.shell.JsonOutput;
import com.github.mirum8.jnscli.shell.Messages;
import com.github.mirum8.jnscli.shell.OutputContext;
import com.github.mirum8.jnscli.shell.OutputMode;
import com.github.mirum8.jnscli.shell.Section;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.shell.ShellPrompter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.concurrent.Callable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PipelineCreateServiceTest {

    private final JenkinsAPI jenkinsAPI = mock(JenkinsAPI.class);
    private final SettingsService settingsService = mock(SettingsService.class);
    private final CommandRunner commandRunner = mock(CommandRunner.class);
    private final ShellPrinter shellPrinter = mock(ShellPrinter.class);
    private final ShellPrompter shellPrompter = mock(ShellPrompter.class);
    private final Messages messages = mock(Messages.class);
    private final Section section = mock(Section.class);
    private final JsonOutput jsonOutput = mock(JsonOutput.class);

    private PipelineCreateService serviceRich;
    private PipelineCreateService serviceJson;

    @BeforeEach
    void setUp() {
        when(settingsService.readSettings()).thenReturn(new Settings("https://jenkins.example", "user", "key"));

        Section.Builder sectionBuilder = mock(Section.Builder.class);
        when(section.builder()).thenReturn(sectionBuilder);
        when(sectionBuilder.field(anyString(), anyString())).thenReturn(sectionBuilder);
        when(sectionBuilder.build()).thenReturn("section");

        serviceRich = new PipelineCreateService(jenkinsAPI, settingsService, commandRunner, shellPrinter,
            shellPrompter, messages, section, new OutputContext(OutputMode.RICH), jsonOutput);
        serviceJson = new PipelineCreateService(jenkinsAPI, settingsService, commandRunner, shellPrinter,
            shellPrompter, messages, section, new OutputContext(OutputMode.JSON), jsonOutput);
    }

    @Test
    void buildXmlUsesDefaultsAndIncludesEverySection() {
        String xml = PipelineCreateService.buildFlowDefinitionXml(
            "https://example.com/foo.git", "main", "Jenkinsfile", null, null);

        assertThat(xml)
            .contains("<flow-definition plugin=\"workflow-job\">")
            .contains("<description></description>")
            .contains("<url>https://example.com/foo.git</url>")
            .contains("<name>*/main</name>")
            .contains("<scriptPath>Jenkinsfile</scriptPath>")
            .doesNotContain("credentialsId");
    }

    @Test
    void buildXmlIncludesCredentialsIdWhenSupplied() {
        String xml = PipelineCreateService.buildFlowDefinitionXml(
            "https://example.com/foo.git", "main", "Jenkinsfile", "git-creds", "desc");

        assertThat(xml)
            .contains("<credentialsId>git-creds</credentialsId>")
            .contains("<description>desc</description>");
    }

    @Test
    void buildXmlEscapesAngleBracketsAmpersandsAndQuotes() {
        String xml = PipelineCreateService.buildFlowDefinitionXml(
            "https://example.com/x?a=b&c=d", "main", "path/<>file", null, "<b>\"a&b\"</b>");

        assertThat(xml)
            .contains("<url>https://example.com/x?a=b&amp;c=d</url>")
            .contains("<scriptPath>path/&lt;&gt;file</scriptPath>")
            .contains("<description>&lt;b&gt;&quot;a&amp;b&quot;&lt;/b&gt;</description>");
    }

    @Test
    void resolveFolderUrlReturnsNullWhenBlank() {
        assertThat(serviceRich.resolveFolderUrl(null)).isNull();
        assertThat(serviceRich.resolveFolderUrl("")).isNull();
        assertThat(serviceRich.resolveFolderUrl("  ")).isNull();
    }

    @Test
    void resolveFolderUrlBuildsJobPathFromSegments() {
        assertThat(serviceRich.resolveFolderUrl("team/sub"))
            .isEqualTo("https://jenkins.example/job/team/job/sub");
    }

    @Test
    void resolveFolderUrlPassesThroughFullUrl() {
        assertThat(serviceRich.resolveFolderUrl("https://jenkins.example/job/team"))
            .isEqualTo("https://jenkins.example/job/team");
    }

    @Test
    void resolveFolderUrlPercentEncodesSegments() {
        assertThat(serviceRich.resolveFolderUrl("my team/sub"))
            .isEqualTo("https://jenkins.example/job/my%20team/job/sub");
    }

    @Test
    void resolveFolderUrlRejectsOffHostUrl() {
        assertThatThrownBy(() -> serviceRich.resolveFolderUrl("http://attacker.example/jenkins"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not match configured Jenkins server");
    }

    @Test
    void resolveFolderUrlRejectsDifferentScheme() {
        assertThatThrownBy(() -> serviceRich.resolveFolderUrl("http://jenkins.example/job/team"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not match configured Jenkins server");
    }

    @Test
    void resolveFolderUrlRejectsDifferentPort() {
        assertThatThrownBy(() -> serviceRich.resolveFolderUrl("https://jenkins.example:9999/job/team"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("does not match configured Jenkins server");
    }

    @Test
    void createInJsonModeEmitsJsonAndSkipsShell() {
        when(jenkinsAPI.createPipelineJob(any(), any(), any())).thenReturn("https://jenkins.example/job/demo");

        serviceJson.create("demo", "https://example.com/r.git", null, null, null, null, null);

        ArgumentCaptor<PipelineCreateService.CreatePipelineJson> captor =
            ArgumentCaptor.forClass(PipelineCreateService.CreatePipelineJson.class);
        verify(jsonOutput).println(captor.capture());
        PipelineCreateService.CreatePipelineJson json = captor.getValue();
        assertThat(json.name()).isEqualTo("demo");
        assertThat(json.branch()).isEqualTo("main");
        assertThat(json.scriptPath()).isEqualTo("Jenkinsfile");
        assertThat(json.url()).isEqualTo("https://jenkins.example/job/demo");
        verify(messages, never()).success(anyString());
    }

    @Test
    void createInRichModePrintsSuccessAndSection() {
        when(jenkinsAPI.createPipelineJob(any(), any(), any())).thenReturn("https://jenkins.example/job/demo");
        when(commandRunner.callWithSpinner(anyString(), any())).thenAnswer(inv -> {
            Callable<?> op = inv.getArgument(1);
            return new Result.Success<>(op.call());
        });

        serviceRich.create("demo", "https://example.com/r.git", null, null, null, null, null);

        verify(messages).success("Pipeline demo created");
        verify(shellPrinter).println("section");
    }

    @Test
    void createForMcpReturnsResultDirectly() {
        when(jenkinsAPI.createPipelineJob(any(), any(), any())).thenReturn("https://jenkins.example/job/demo");

        PipelineCreateService.CreatePipelineJson result = serviceRich.createForMcp(
            "demo", "https://example.com/r.git", "develop", "ci/Jenkinsfile", null, null, "desc");

        assertThat(result.name()).isEqualTo("demo");
        assertThat(result.branch()).isEqualTo("develop");
        assertThat(result.scriptPath()).isEqualTo("ci/Jenkinsfile");
        assertThat(result.url()).isEqualTo("https://jenkins.example/job/demo");
    }

    @Test
    void createInJsonModeRejectsBlankRequiredFields() {
        assertThatThrownBy(() -> serviceJson.create(null, "https://example.com/r.git", null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("name");
        assertThatThrownBy(() -> serviceJson.create("demo", "", null, null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("repo");
    }
}
