package com.github.mirum8.jnscli;

import com.github.mirum8.jnscli.mcp.JshellMcpApplication;
import com.github.mirum8.jnscli.settings.SettingsProperties;
import com.github.mirum8.jnscli.shell.OutputContext;
import com.github.mirum8.jnscli.shell.OutputMode;
import org.jline.utils.AttributedString;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.McpServerObjectMapperAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.ToolCallbackConverterAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.annotations.McpServerAnnotationScannerAutoConfiguration;
import org.springframework.ai.mcp.server.common.autoconfigure.annotations.McpServerSpecificationFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.shell.command.annotation.CommandScan;
import org.springframework.shell.jline.PromptProvider;

import java.util.ArrayList;
import java.util.List;

@SpringBootApplication(exclude = {
    McpServerAutoConfiguration.class,
    McpServerObjectMapperAutoConfiguration.class,
    ToolCallbackConverterAutoConfiguration.class,
    McpServerAnnotationScannerAutoConfiguration.class,
    McpServerSpecificationFactoryAutoConfiguration.class
})
@ComponentScan(excludeFilters = @ComponentScan.Filter(
    type = FilterType.ASSIGNABLE_TYPE,
    classes = JshellMcpApplication.class))
@CommandScan
@EnableConfigurationProperties(SettingsProperties.class)
public class JshellApplication implements PromptProvider {

    public static final String MCP_PROFILE = "mcp";
    public static final String MCP_ALLOWED_JOBS_PROPERTY = "jns.mcp.allowed.jobs";

    public static void main(String[] args) {
        String[] cleanedArgs = extractOutputMode(args);
        new SpringApplicationBuilder(JshellApplication.class).run(cleanedArgs);
    }

    static String[] extractOutputMode(String[] args) {
        List<String> rest = new ArrayList<>(args.length);
        String mode = null;
        int i = 0;
        while (i < args.length) {
            String arg = args[i];
            if (arg.startsWith("--output=")) {
                mode = arg.substring("--output=".length());
                i++;
            } else if ("--output".equals(arg) && i + 1 < args.length) {
                mode = args[i + 1];
                i += 2;
            } else {
                rest.add(arg);
                i++;
            }
        }
        if (mode != null) {
            OutputMode.parse(mode);
            System.setProperty(OutputContext.MODE_PROPERTY, mode);
        }
        return rest.toArray(new String[0]);
    }

    @Override
    public AttributedString getPrompt() {
        return new AttributedString("jns:>");
    }
}
