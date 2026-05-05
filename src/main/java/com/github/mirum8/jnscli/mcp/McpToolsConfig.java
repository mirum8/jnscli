package com.github.mirum8.jnscli.mcp;

import com.github.mirum8.jnscli.JshellApplication;
import com.github.mirum8.jnscli.alias.AliasService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile(JshellApplication.MCP_PROFILE)
class McpToolsConfig {

    @Bean
    AllowedJobs allowedJobs(AliasService aliasService) {
        return new AllowedJobs(System.getProperty(JshellApplication.MCP_ALLOWED_JOBS_PROPERTY), aliasService);
    }

    @Bean
    ToolCallbackProvider mcpToolCallbacks(McpTools tools) {
        return MethodToolCallbackProvider.builder()
            .toolObjects(tools)
            .build();
    }
}
