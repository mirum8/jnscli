package com.github.mirum8.jnscli.mcp;

import com.github.mirum8.jnscli.JshellApplication;
import com.github.mirum8.jnscli.settings.SettingsProperties;
import com.github.mirum8.jnscli.shell.OutputContext;
import com.github.mirum8.jnscli.shell.OutputMode;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;

@SpringBootApplication
@ComponentScan(
    basePackages = "com.github.mirum8.jnscli",
    excludeFilters = @ComponentScan.Filter(
        type = FilterType.ASSIGNABLE_TYPE,
        classes = JshellApplication.class))
@EnableConfigurationProperties(SettingsProperties.class)
public class JshellMcpApplication {

    public static void main(String[] args) {
        applyArgsToSystemProperties(args);
        new SpringApplicationBuilder(JshellMcpApplication.class)
            .profiles(JshellApplication.MCP_PROFILE)
            .run();
    }

    static void applyArgsToSystemProperties(String[] args) {
        if (args.length > 0) {
            System.setProperty(JshellApplication.MCP_ALLOWED_JOBS_PROPERTY, String.join(",", args));
        }
        System.setProperty(OutputContext.MODE_PROPERTY, OutputMode.JSON.name().toLowerCase());
    }
}
