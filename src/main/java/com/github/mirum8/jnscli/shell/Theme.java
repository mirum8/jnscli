package com.github.mirum8.jnscli.shell;

import org.jline.utils.AttributedStringBuilder;
import org.jline.utils.AttributedStyle;
import org.springframework.stereotype.Component;

@Component
public class Theme {
    private final TerminalCapabilities caps;

    public Theme(TerminalCapabilities caps) {
        this.caps = caps;
    }

    public String label(String s) {
        return styled(s, AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN));
    }

    public String header(String s) {
        return styled(s, AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW).bold());
    }

    public String success(String s) {
        return styled(s, AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN));
    }

    public String failure(String s) {
        return styled(s, AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
    }

    public String warning(String s) {
        return styled(s, AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
    }

    public String accent(String s) {
        return styled(s, AttributedStyle.DEFAULT.foreground(AttributedStyle.MAGENTA));
    }

    public String dim(String s) {
        return styled(s, AttributedStyle.DEFAULT.faint());
    }

    public String bold(String s) {
        return styled(s, AttributedStyle.DEFAULT.bold());
    }

    private String styled(String s, AttributedStyle style) {
        if (s == null) {
            return null;
        }
        if (!caps.supportsAnsi()) {
            return s;
        }
        return new AttributedStringBuilder().append(s, style).toAnsi();
    }
}
