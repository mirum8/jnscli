package com.github.mirum8.jnscli.mcp;

import com.github.mirum8.jnscli.JshellApplication;
import com.github.mirum8.jnscli.shell.JsonOutput;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile(JshellApplication.MCP_PROFILE)
class McpOverridesConfig {

    @Bean
    @Primary
    JsonOutput mcpJsonOutput(McpJsonCapture capture) {
        return new McpJsonOutput(capture);
    }

    @Bean
    @Primary
    ShellPrinter mcpShellPrinter() {
        return new NoOpShellPrinter();
    }
}
