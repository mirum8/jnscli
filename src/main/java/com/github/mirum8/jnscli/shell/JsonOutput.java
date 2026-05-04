package com.github.mirum8.jnscli.shell;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.mirum8.jnscli.jenkins.JenkinsApiUtils;
import org.springframework.stereotype.Component;

@Component
public class JsonOutput {
    private final ShellPrinter printer;
    private final ObjectMapper mapper = JenkinsApiUtils.createObjectMapper();

    public JsonOutput(ShellPrinter printer) {
        this.printer = printer;
    }

    public void println(Object value) {
        try {
            printer.println(mapper.writeValueAsString(value));
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize JSON output", e);
        }
    }
}
