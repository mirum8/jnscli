package com.github.mirum8.jnscli.shell;

import org.jline.terminal.Terminal;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class TerminalCapabilities {
    private static final int FALLBACK_WIDTH = 80;

    private final Terminal terminal;
    private Boolean ansi;
    private Boolean unicode;
    private Integer width;

    public TerminalCapabilities(@Lazy Terminal terminal) {
        this.terminal = terminal;
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
            String noColor = System.getenv("NO_COLOR");
            if (noColor != null && !noColor.isEmpty()) {
                ansi = false;
            } else {
                String term = System.getenv("TERM");
                String jlineType = terminal.getType();
                ansi = (term != null && !"dumb".equalsIgnoreCase(term))
                    || (jlineType != null && !"dumb".equalsIgnoreCase(jlineType));
            }
        }
        return ansi;
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
