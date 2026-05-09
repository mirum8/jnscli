package com.github.mirum8.jnscli.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mirum8.jnscli.abort.AbortService;
import com.github.mirum8.jnscli.ai.AiService;
import com.github.mirum8.jnscli.alias.AliasService;
import com.github.mirum8.jnscli.common.JobDescriptorProvider;
import com.github.mirum8.jnscli.creds.CredsFileWriter;
import com.github.mirum8.jnscli.creds.CredentialType;
import com.github.mirum8.jnscli.creds.PasswordGenerator;
import com.github.mirum8.jnscli.diagnose.ErrorService;
import com.github.mirum8.jnscli.info.InfoService;
import com.github.mirum8.jnscli.jenkins.Credential;
import com.github.mirum8.jnscli.jenkins.CredentialsAPI;
import com.github.mirum8.jnscli.jenkins.JenkinsAPI;
import com.github.mirum8.jnscli.list.ListService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class McpToolsCredentialsTest {

    private final CredentialsAPI credentialsAPI = mock(CredentialsAPI.class);
    private final PasswordGenerator passwordGenerator = mock(PasswordGenerator.class);
    private final CredsFileWriter credsFileWriter = mock(CredsFileWriter.class);
    private final AliasService aliasService = mock(AliasService.class);

    private McpTools tools;

    @BeforeEach
    void setUp() {
        tools = new McpTools(
            mock(ListService.class),
            mock(InfoService.class),
            mock(ErrorService.class),
            mock(AbortService.class),
            mock(AiService.class),
            mock(JenkinsAPI.class),
            mock(JobDescriptorProvider.class),
            new AllowedJobs(null, aliasService),
            new McpJsonCapture(),
            credentialsAPI,
            passwordGenerator,
            credsFileWriter);
    }

    @Test
    void listCredentialsSerializesApiResultToJson() throws Exception {
        when(credentialsAPI.list()).thenReturn(List.of(
            new Credential("deploy-bot", "CI bot", "Username with password"),
            new Credential("api-key", "", "Secret text")));

        String json = tools.listCredentials();

        assertThat(new ObjectMapper().readTree(json)).hasSize(2);
        assertThat(json).contains("deploy-bot").contains("api-key");
    }

    @Test
    void createUserPassWithExplicitPasswordSkipsGeneratorAndFileWriter() throws Exception {
        McpTools.CredentialCreated result = tools.createUserPassCredential("known", "ci", "explicit-value", false, null, null);

        verify(credentialsAPI).createUserPass("known", "ci", "explicit-value", null, "GLOBAL");
        verifyNoInteractions(passwordGenerator);
        verifyNoInteractions(credsFileWriter);
        assertThat(result.id()).isEqualTo("known");
        assertThat(result.type()).isEqualTo("UsernamePassword");
        assertThat(result.filePath()).isNull();

        String json = new ObjectMapper().writeValueAsString(result);
        assertThat(json).doesNotContain("explicit-value").doesNotContain("password");
    }

    @Test
    void createUserPassWithRandomGeneratesPasswordWritesFileAndDoesNotLeakPasswordInResponse() throws Exception {
        when(passwordGenerator.generate()).thenReturn("Gen3r4t3dPwd!_RandomXYZ_-");
        when(credsFileWriter.write(eq("ci-bot"), eq(CredentialType.USER_PASS), any()))
            .thenReturn(Path.of("/tmp/creds/ci-bot-20260509-143022.txt"));

        McpTools.CredentialCreated result = tools.createUserPassCredential("ci-bot", "ci", null, true, null, null);

        verify(credentialsAPI).createUserPass("ci-bot", "ci", "Gen3r4t3dPwd!_RandomXYZ_-", null, "GLOBAL");
        verify(credsFileWriter).write(eq("ci-bot"), eq(CredentialType.USER_PASS), any());
        assertThat(result.filePath()).isEqualTo("/tmp/creds/ci-bot-20260509-143022.txt");

        String json = new ObjectMapper().writeValueAsString(result);
        assertThat(json).doesNotContain("Gen3r4t3dPwd!_RandomXYZ_-");
    }

    @Test
    void createUserPassRejectsBothPasswordAndRandom() {
        assertThatThrownBy(() -> tools.createUserPassCredential("x", "ci", "p", true, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("mutually exclusive");
        verifyNoInteractions(credentialsAPI);
    }

    @Test
    void createUserPassRejectsNeitherPasswordNorRandom() {
        assertThatThrownBy(() -> tools.createUserPassCredential("x", "ci", null, false, null, null))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(credentialsAPI);
    }

    @Test
    void createSecretTextWithRandomWritesFileAndDoesNotLeakSecret() throws Exception {
        when(passwordGenerator.generate()).thenReturn("S3cr3tValueXYZ_!@#$");
        when(credsFileWriter.write(eq("api-key"), eq(CredentialType.SECRET_TEXT), any()))
            .thenReturn(Path.of("/tmp/creds/api-key.txt"));

        McpTools.CredentialCreated result = tools.createSecretTextCredential("api-key", null, true, null, null);

        verify(credentialsAPI).createSecretText("api-key", "S3cr3tValueXYZ_!@#$", null, "GLOBAL");
        verify(credsFileWriter).write(eq("api-key"), eq(CredentialType.SECRET_TEXT), any());
        assertThat(result.type()).isEqualTo("SecretText");
        assertThat(result.filePath()).isEqualTo("/tmp/creds/api-key.txt");

        String json = new ObjectMapper().writeValueAsString(result);
        assertThat(json).doesNotContain("S3cr3tValueXYZ_!@#$");
    }

    @Test
    void createSecretTextWithExplicitSecretWritesNoFile() {
        McpTools.CredentialCreated result = tools.createSecretTextCredential("known", "explicit", false, null, null);

        verify(credentialsAPI).createSecretText("known", "explicit", null, "GLOBAL");
        verifyNoInteractions(credsFileWriter);
        verify(passwordGenerator, never()).generate();
        assertThat(result.filePath()).isNull();
    }

    @Test
    void deleteCredentialDelegatesToApiAndReturnsSuccessMessage() {
        String result = tools.deleteCredential("ci-bot");

        verify(credentialsAPI).delete("ci-bot");
        assertThat(result).isEqualTo("Deleted credential 'ci-bot'");
    }

    @Test
    void createUserPassRejectsPathTraversalId() {
        assertThatThrownBy(() -> tools.createUserPassCredential("../../etc/passwd", "ci", "p", false, null, null))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(credentialsAPI);
        verifyNoInteractions(credsFileWriter);
    }

    @Test
    void deleteCredentialRejectsPathTraversalId() {
        assertThatThrownBy(() -> tools.deleteCredential("../../etc/passwd"))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(credentialsAPI);
    }
}
