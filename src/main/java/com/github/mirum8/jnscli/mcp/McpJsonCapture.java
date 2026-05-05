package com.github.mirum8.jnscli.mcp;

import com.github.mirum8.jnscli.JshellApplication;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile(JshellApplication.MCP_PROFILE)
public class McpJsonCapture {

    private final ThreadLocal<String> last = new ThreadLocal<>();

    public void clear() {
        last.remove();
    }

    public void set(String json) {
        last.set(json);
    }

    public String get() {
        return last.get();
    }
}
