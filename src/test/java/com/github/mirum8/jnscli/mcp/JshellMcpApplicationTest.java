package com.github.mirum8.jnscli.mcp;

import com.github.mirum8.jnscli.JshellApplication;
import com.github.mirum8.jnscli.shell.OutputContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JshellMcpApplicationTest {

    @BeforeEach
    @AfterEach
    void clearProperties() {
        System.clearProperty(OutputContext.MODE_PROPERTY);
        System.clearProperty(JshellApplication.MCP_ALLOWED_JOBS_PROPERTY);
    }

    @Test
    void noArgsLeavesAllowlistUnsetAndForcesJsonOutput() {
        JshellMcpApplication.applyArgsToSystemProperties(new String[]{});

        assertThat(System.getProperty(JshellApplication.MCP_ALLOWED_JOBS_PROPERTY)).isNull();
        assertThat(System.getProperty(OutputContext.MODE_PROPERTY)).isEqualTo("json");
    }

    @Test
    void multipleArgsBecomeCsvAllowlist() {
        JshellMcpApplication.applyArgsToSystemProperties(new String[]{"job-a", "job-b"});

        assertThat(System.getProperty(JshellApplication.MCP_ALLOWED_JOBS_PROPERTY)).isEqualTo("job-a,job-b");
        assertThat(System.getProperty(OutputContext.MODE_PROPERTY)).isEqualTo("json");
    }

    @Test
    void singleArgBecomesAllowlistOfOne() {
        JshellMcpApplication.applyArgsToSystemProperties(new String[]{"job-a"});

        assertThat(System.getProperty(JshellApplication.MCP_ALLOWED_JOBS_PROPERTY)).isEqualTo("job-a");
    }
}
