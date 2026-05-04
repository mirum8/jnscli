package com.github.mirum8.jnscli.shell;

import org.jline.terminal.Terminal;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class TerminalCapabilities {
    private static final int FALLBACK_WIDTH = 80;

    private final Terminal terminal;
    private final OutputContext outputContext;
    private Boolean ansi;
    private Boolean unicode;
    private Integer width;

    public TerminalCapabilities(@Lazy Terminal terminal, OutputContext outputContext) {
        this.terminal = terminal;
        this.outputContext = outputContext;
    }

    public int width() {
        if (width == null) {
            int columns = terminal.getSize().getColumns();
            width = columns > 0 ? columns : FALLBACK_WIDTH;
        }
        return width;
    }

    public boolean supportsAnsi() {
        if (ansi == null) {
            ansi = computeAnsi();
        }
        return ansi;
    }

    private boolean computeAnsi() {
        if (outputContext != null && !outputContext.isRich()) {
            return false;
        }
        String noColor = System.getenv("NO_COLOR");
        if (noColor != null && !noColor.isEmpty()) {
            return false;
        }
        String term = System.getenv("TERM");
        String jlineType = terminal.getType();
        return (term != null && !"dumb".equalsIgnoreCase(term))
            || (jlineType != null && !"dumb".equalsIgnoreCase(jlineType));
    }

    public boolean supportsUnicode() {
        if (unicode == null) {
            if (!supportsAnsi()) {
                unicode = false;
            } else {
                unicode = containsUtf(System.getenv("LANG"))
                    || containsUtf(System.getenv("LC_ALL"))
                    || containsUtf(System.getenv("LC_CTYPE"));
            }
        }
        return unicode;
    }

    private static boolean containsUtf(String s) {
        return s != null && s.toLowerCase().contains("utf");
    }
}
