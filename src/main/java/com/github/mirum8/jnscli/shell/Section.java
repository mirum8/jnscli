package com.github.mirum8.jnscli.shell;

import org.springframework.stereotype.Component;

@Component
public class Section {
    private final Theme theme;
    private final TerminalCapabilities caps;

    public Section(Theme theme, TerminalCapabilities caps) {
        this.theme = theme;
        this.caps = caps;
    }

    public Builder builder() {
        return new Builder(theme, caps);
    }

    public static final class Builder {
        private final Theme theme;
        private final TerminalCapabilities caps;
        private final StringBuilder sb = new StringBuilder();

        private Builder(Theme theme, TerminalCapabilities caps) {
            this.theme = theme;
            this.caps = caps;
        }

        public Builder header(String title) {
            sb.append(theme.header(title)).append("\n");
            return this;
        }

        public Builder field(String label, String value) {
            sb.append("  ").append(theme.label(label + ": ")).append(value).append("\n");
            return this;
        }

        public Builder line(String s) {
            sb.append(s).append("\n");
            return this;
        }

        public Builder blank() {
            sb.append("\n");
            return this;
        }

        public Builder divider() {
            if (!caps.supportsAnsi()) {
                sb.append("\n");
                return this;
            }
            int width = Math.min(caps.width(), 60);
            String fill = caps.supportsUnicode() ? "─" : "-";
            sb.append(theme.dim(fill.repeat(Math.max(1, width)))).append("\n");
            return this;
        }

        public String build() {
            return sb.toString();
        }
    }
}
