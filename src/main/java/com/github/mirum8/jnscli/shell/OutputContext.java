package com.github.mirum8.jnscli.shell;

import org.springframework.stereotype.Component;

@Component
public class OutputContext {
    public static final String MODE_PROPERTY = "jns.output.mode";

    private final OutputMode mode;

    public OutputContext() {
        this(System.getProperty(MODE_PROPERTY), System.getenv());
    }

    public OutputContext(OutputMode mode) {
        this.mode = mode == null ? OutputMode.RICH : mode;
    }

    OutputContext(String explicitMode, java.util.Map<String, String> env) {
        OutputMode parsed = OutputMode.parse(explicitMode);
        if (parsed == null) {
            parsed = explicitMode == null ? autoDetect(env) : OutputMode.RICH;
        }
        this.mode = parsed;
    }

    public OutputMode mode() {
        return mode;
    }

    public boolean isRich() {
        return mode == OutputMode.RICH;
    }

    public boolean isJson() {
        return mode == OutputMode.JSON;
    }

    public boolean isPlain() {
        return mode == OutputMode.PLAIN;
    }

    private static OutputMode autoDetect(java.util.Map<String, String> env) {
        if (notBlank(env.get("CLAUDECODE")) || notBlank(env.get("CLAUDE_CODE")) || notBlank(env.get("CI"))) {
            return OutputMode.PLAIN;
        }
        if (System.console() == null) {
            return OutputMode.PLAIN;
        }
        return OutputMode.RICH;
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isEmpty();
    }
}
