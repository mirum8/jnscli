package com.github.mirum8.jnscli.list;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.github.mirum8.jnscli.shell.TextColor.GREEN;
import static com.github.mirum8.jnscli.shell.TextColor.RED;
import static com.github.mirum8.jnscli.shell.TextColor.YELLOW;
import static com.github.mirum8.jnscli.shell.TextFormatter.colored;

@Component
class JobListTableFormatter {

    List<String> createJobTable(List<JobRow> jobs) {
        List<String> headers = Arrays.asList("ID", "St", "Name");
        List<List<Object>> rows = jobs.stream()
            .map(row -> List.of(
                String.valueOf(row.id()),
                colorize(row.color()),
                row.name()))
            .toList();
        int[] maxLengths = new int[headers.size()];
        for (int i = 0; i < headers.size(); i++) {
            maxLengths[i] = headers.get(i).length();
        }
        for (List<Object> row : rows) {
            for (int i = 0; i < row.size(); i++) {
                if (i == 1) {
                    continue;
                }
                int rowLength = switch (row.get(i)) {
                    case String s -> s.length();
                    case Symbol.Double ignored -> 2;
                    default -> 0;
                };
                maxLengths[i] = Math.max(maxLengths[i], rowLength);
            }
        }

        try {
            int lineSize = Arrays.stream(maxLengths).sum() + maxLengths.length * 2 + 1;
            List<String> result = new ArrayList<>();
            result.add(formatLine(lineSize));
            result.add(formatHeaders(headers, maxLengths));
            for (List<Object> row : rows) {
                List<RowItem> rowItems = row.stream().map(RowItem::new).toList();
                result.add(formatRow(rowItems, maxLengths));
            }
            result.add(formatLine(lineSize));
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String formatLine(int lengthSum) {
        return "  " + "-".repeat(lengthSum);
    }

    private String formatHeaders(List<String> headers, int[] maxLengths) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < headers.size(); i++) {
            String cellContent = headers.get(i);
            result.append("  ").append(cellContent);
            result.append(" ".repeat(Math.max(0, maxLengths[i] - cellContent.length())));
            result.append(" ");
        }
        return result.toString();
    }

    private Symbol colorize(Symbol jobColor) {
        return switch (jobColor.value()) {
            case String s when s.startsWith("✔") -> copyWithValue(jobColor, colored(jobColor.value(), GREEN));
            case String s when s.startsWith("✘") -> copyWithValue(jobColor, colored(jobColor.value(), RED));
            case String s when s.startsWith("!") -> copyWithValue(jobColor, colored(jobColor.value(), YELLOW));
            case String s when s.startsWith("A") -> copyWithValue(jobColor, colored(jobColor.value(), YELLOW));
            case String s when s.startsWith("D") -> copyWithValue(jobColor, colored(jobColor.value(), YELLOW));
            case String s when s.startsWith("?") -> copyWithValue(jobColor, colored(jobColor.value(), YELLOW));
            case String s when s.startsWith("N") -> copyWithValue(jobColor, colored(jobColor.value(), YELLOW));
            default -> jobColor;
        };
    }

    private Symbol copyWithValue(Symbol color, String value) {
        return switch (color) {
            case Symbol.Double ignored -> new Symbol.Double(value);
            case Symbol.Single ignored -> new Symbol.Single(value);
        };
    }

    private String formatRow(List<RowItem> row, int[] maxLengths) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < row.size(); i++) {
            RowItem rowItem = row.get(i);
            String cellContent = switch (rowItem.content()) {
                case String s -> s;
                case Symbol u -> u.value();
                default -> "";
            };
            result.append("  ").append(cellContent);
            int cellContentLength = rowItem.length();
            result.append(" ".repeat(Math.max(0, maxLengths[i] - cellContentLength)));
            result.append(" ");
        }
        return result.toString();
    }

    private record RowItem(Object content, int length) {
        public RowItem(Object content) {
            this(content, switch (content) {
                case String s -> s.length();
                case Symbol.Double ignored -> 2;
                case Symbol.Single ignored -> 1;
                default -> 0;
            });
        }
    }
}
