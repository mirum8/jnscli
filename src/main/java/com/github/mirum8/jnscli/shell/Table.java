package com.github.mirum8.jnscli.shell;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class Table {
    private static final Pattern ANSI_PATTERN = Pattern.compile("\u001B\\[[0-9;]*m");
    private static final int COLUMN_GAP = 2;

    public enum Align {LEFT, RIGHT, CENTER}

    public sealed interface Width permits Auto, Fixed, Truncate {
    }

    public record Auto() implements Width {
    }

    public record Fixed(int value) implements Width {
    }

    public record Truncate(int min) implements Width {
    }

    public record Column(String header, Align align, Width width) {
        public static Column of(String header) {
            return new Column(header, Align.LEFT, new Auto());
        }

        public static Column truncate(String header, int minWidth) {
            return new Column(header, Align.LEFT, new Truncate(minWidth));
        }
    }

    private final TerminalCapabilities caps;

    public Table(TerminalCapabilities caps) {
        this.caps = caps;
    }

    public static int displayWidth(String s) {
        if (s == null) {
            return 0;
        }
        return ANSI_PATTERN.matcher(s).replaceAll("").length();
    }

    public List<String> render(List<Column> cols, List<List<String>> rows) {
        int[] widths = computeWidths(cols, rows);
        List<String> result = new ArrayList<>(rows.size() + 1);
        result.add(formatRow(cols.stream().map(Column::header).toList(), widths, cols));
        for (List<String> row : rows) {
            result.add(formatRow(row, widths, cols));
        }
        return result;
    }

    private int[] computeWidths(List<Column> cols, List<List<String>> rows) {
        int[] widths = new int[cols.size()];
        for (int i = 0; i < cols.size(); i++) {
            widths[i] = displayWidth(cols.get(i).header());
        }
        for (List<String> row : rows) {
            for (int i = 0; i < cols.size(); i++) {
                widths[i] = Math.max(widths[i], displayWidth(row.get(i)));
            }
        }
        for (int i = 0; i < cols.size(); i++) {
            if (cols.get(i).width() instanceof Fixed(int value)) {
                widths[i] = value;
            }
        }
        int totalWidth = totalWidth(widths);
        int terminalWidth = caps.width();
        if (totalWidth > terminalWidth) {
            int over = totalWidth - terminalWidth;
            for (int i = 0; i < cols.size() && over > 0; i++) {
                if (cols.get(i).width() instanceof Truncate(int min)) {
                    int shrinkable = widths[i] - min;
                    if (shrinkable > 0) {
                        int shrink = Math.min(shrinkable, over);
                        widths[i] -= shrink;
                        over -= shrink;
                    }
                }
            }
        }
        return widths;
    }

    private static int totalWidth(int[] widths) {
        int total = 0;
        for (int w : widths) {
            total += w;
        }
        total += COLUMN_GAP * Math.max(0, widths.length - 1);
        total += 2;
        return total;
    }

    private String formatRow(List<String> cells, int[] widths, List<Column> cols) {
        StringBuilder sb = new StringBuilder("  ");
        for (int i = 0; i < cells.size(); i++) {
            String cell = cells.get(i) == null ? "" : cells.get(i);
            int width = widths[i];
            int dw = displayWidth(cell);
            String content = dw > width ? truncate(cell, width) : cell;
            int padding = Math.max(0, width - displayWidth(content));
            switch (cols.get(i).align()) {
                case LEFT -> sb.append(content).append(spaces(padding));
                case RIGHT -> sb.append(spaces(padding)).append(content);
                case CENTER -> {
                    int leftPad = padding / 2;
                    sb.append(spaces(leftPad)).append(content).append(spaces(padding - leftPad));
                }
            }
            if (i < cells.size() - 1) {
                sb.append(spaces(COLUMN_GAP));
            }
        }
        return sb.toString();
    }

    private String truncate(String cell, int width) {
        String stripped = ANSI_PATTERN.matcher(cell).replaceAll("");
        String ellipsis = caps.supportsUnicode() ? "…" : "...";
        int ellipsisWidth = ellipsis.length();
        if (width <= ellipsisWidth) {
            return stripped.substring(0, Math.min(stripped.length(), width));
        }
        return stripped.substring(0, width - ellipsisWidth) + ellipsis;
    }

    private static String spaces(int n) {
        return n > 0 ? " ".repeat(n) : "";
    }
}
