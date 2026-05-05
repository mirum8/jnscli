package com.github.mirum8.jnscli;

import com.github.mirum8.jnscli.settings.SettingsProperties;
import com.github.mirum8.jnscli.shell.OutputContext;
import com.github.mirum8.jnscli.shell.OutputMode;
import org.jline.utils.AttributedString;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.shell.command.annotation.CommandScan;
import org.springframework.shell.jline.PromptProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootApplication
@CommandScan
@EnableConfigurationProperties(SettingsProperties.class)
public class JshellApplication implements PromptProvider {

    public static final String MCP_PROFILE = "mcp";
    public static final String MCP_ALLOWED_JOBS_PROPERTY = "jns.mcp.allowed.jobs";

    public static void main(String[] args) {
        String[] cleanedArgs = extractOutputMode(args);
        McpInvocation mcp = extractMcpMode(cleanedArgs);
        SpringApplicationBuilder builder = new SpringApplicationBuilder(JshellApplication.class);
        if (mcp.active) {
            builder.profiles(MCP_PROFILE);
        }
        builder.run(mcp.remainingArgs.toArray(new String[0]));
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

    static McpInvocation extractMcpMode(String[] args) {
        if (args.length == 0 || !"mcp".equals(args[0])) {
            return new McpInvocation(false, List.of(args));
        }
        List<String> allowed = Arrays.asList(args).subList(1, args.length);
        if (!allowed.isEmpty()) {
            System.setProperty(MCP_ALLOWED_JOBS_PROPERTY, String.join(",", allowed));
        }
        System.setProperty(OutputContext.MODE_PROPERTY, OutputMode.JSON.name().toLowerCase());
        return new McpInvocation(true, List.of());
    }

    record McpInvocation(boolean active, List<String> remainingArgs) {
    }

    @Override
    public AttributedString getPrompt() {
        return new AttributedString("jns:>");
    }
}
