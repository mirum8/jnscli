package com.github.mirum8.jnscli.shell;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

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
        private sealed interface Entry permits HeaderEntry, FieldEntry, LineEntry, BlankEntry, DividerEntry {
        }

        private record HeaderEntry(String title) implements Entry {
        }

        private record FieldEntry(String label, String value) implements Entry {
        }

        private record LineEntry(String text) implements Entry {
        }

        private record BlankEntry() implements Entry {
        }

        private record DividerEntry() implements Entry {
        }

        private final Theme theme;
        private final TerminalCapabilities caps;
        private final List<Entry> entries = new ArrayList<>();

        private Builder(Theme theme, TerminalCapabilities caps) {
            this.theme = theme;
            this.caps = caps;
        }

        public Builder header(String title) {
            entries.add(new HeaderEntry(title));
            return this;
        }

        public Builder field(String label, String value) {
            entries.add(new FieldEntry(label, value));
            return this;
        }

        public Builder line(String s) {
            entries.add(new LineEntry(s));
            return this;
        }

        public Builder blank() {
            entries.add(new BlankEntry());
            return this;
        }

        public Builder divider() {
            entries.add(new DividerEntry());
            return this;
        }

        public String build() {
            int labelWidth = 0;
            for (Entry e : entries) {
                if (e instanceof FieldEntry(String label, String ignored)) {
                    labelWidth = Math.max(labelWidth, label.length());
                }
            }
            StringBuilder sb = new StringBuilder();
            for (Entry e : entries) {
                switch (e) {
                    case HeaderEntry(String title) -> sb.append(theme.header(title)).append("\n");
                    case FieldEntry(String label, String value) ->
                        sb.append("  ").append(theme.label(padRight(label, labelWidth) + ": ")).append(value).append("\n");
                    case LineEntry(String text) -> sb.append(text).append("\n");
                    case BlankEntry ignored -> sb.append("\n");
                    case DividerEntry ignored -> sb.append(renderDivider()).append("\n");
                }
            }
            return sb.toString();
        }

        private String renderDivider() {
            if (!caps.supportsAnsi()) {
                return "";
            }
            int width = Math.min(caps.width(), 60);
            String fill = caps.supportsUnicode() ? "─" : "-";
            return theme.dim(fill.repeat(Math.max(1, width)));
        }

        private static String padRight(String s, int width) {
            int gap = Math.max(0, width - s.length());
            return gap == 0 ? s : s + " ".repeat(gap);
        }
    }
}
