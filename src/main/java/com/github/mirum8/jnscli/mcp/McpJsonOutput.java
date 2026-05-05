package com.github.mirum8.jnscli.mcp;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mirum8.jnscli.jenkins.JenkinsApiUtils;
import com.github.mirum8.jnscli.shell.JsonOutput;

class McpJsonOutput extends JsonOutput {

    private final ObjectMapper mapper = JenkinsApiUtils.createObjectMapper();
    private final McpJsonCapture capture;

    McpJsonOutput(McpJsonCapture capture) {
        super(null);
        this.capture = capture;
    }

    @Override
    public void println(Object value) {
        try {
            capture.set(mapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON output", e);
        }
    }
}
