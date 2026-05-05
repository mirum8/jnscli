package com.github.mirum8.jnscli;

import com.github.mirum8.jnscli.shell.OutputContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JshellApplicationTest {

    @BeforeEach
    @AfterEach
    void clearProperty() {
        System.clearProperty(OutputContext.MODE_PROPERTY);
        System.clearProperty(JshellApplication.MCP_ALLOWED_JOBS_PROPERTY);
    }

    @Test
    void stripsOutputEqualsFlagAndSetsProperty() {
        String[] result = JshellApplication.extractOutputMode(new String[]{"--output=json", "build", "my-job"});
        assertThat(result).containsExactly("build", "my-job");
        assertThat(System.getProperty(OutputContext.MODE_PROPERTY)).isEqualTo("json");
    }

    @Test
    void stripsTwoTokenOutputFlag() {
        String[] result = JshellApplication.extractOutputMode(new String[]{"--output", "plain", "list"});
        assertThat(result).containsExactly("list");
        assertThat(System.getProperty(OutputContext.MODE_PROPERTY)).isEqualTo("plain");
    }

    @Test
    void leavesUnrelatedArgsUntouched() {
        String[] result = JshellApplication.extractOutputMode(new String[]{"build", "my-job", "--params", "x=1"});
        assertThat(result).containsExactly("build", "my-job", "--params", "x=1");
        assertThat(System.getProperty(OutputContext.MODE_PROPERTY)).isNull();
    }

    @Test
    void rejectsUnknownMode() {
        assertThatThrownBy(() -> JshellApplication.extractOutputMode(new String[]{"--output=loud"}))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void mcpModeWithoutAllowlistIsUnrestricted() {
        JshellApplication.McpInvocation result = JshellApplication.extractMcpMode(new String[]{"mcp"});
        assertThat(result.active()).isTrue();
        assertThat(result.remainingArgs()).isEmpty();
        assertThat(System.getProperty(JshellApplication.MCP_ALLOWED_JOBS_PROPERTY)).isNull();
        assertThat(System.getProperty(OutputContext.MODE_PROPERTY)).isEqualTo("json");
    }

    @Test
    void mcpModeWithAllowlistJoinsArgsAsCsv() {
        JshellApplication.McpInvocation result = JshellApplication.extractMcpMode(new String[]{"mcp", "job-a", "job-b"});
        assertThat(result.active()).isTrue();
        assertThat(result.remainingArgs()).isEmpty();
        assertThat(System.getProperty(JshellApplication.MCP_ALLOWED_JOBS_PROPERTY)).isEqualTo("job-a,job-b");
    }

    @Test
    void nonMcpArgsAreLeftAlone() {
        JshellApplication.McpInvocation result = JshellApplication.extractMcpMode(new String[]{"build", "job-a"});
        assertThat(result.active()).isFalse();
        assertThat(result.remainingArgs()).containsExactly("build", "job-a");
        assertThat(System.getProperty(JshellApplication.MCP_ALLOWED_JOBS_PROPERTY)).isNull();
    }
}
