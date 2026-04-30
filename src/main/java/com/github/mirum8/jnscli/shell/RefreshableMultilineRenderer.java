package com.github.mirum8.jnscli.shell;

import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class RefreshableMultilineRenderer {
    private int linesToRemove;
    private final Terminal terminal;
    private final TerminalCapabilities caps;

    public RefreshableMultilineRenderer(@Lazy Terminal terminal, TerminalCapabilities caps) {
        this.terminal = terminal;
        this.caps = caps;
    }

    public void render(String line) {
        if (line == null) {
            return;
        }
        var lines = line.split("\n");
        render(List.of(lines));
    }

    public synchronized void render(List<String> lines) {
        if (caps.supportsAnsi()) {
            clean(linesToRemove);
            linesToRemove = lines.size();
        }
        int width = caps.width();
        for (String s : lines) {
            terminal.writer().println(truncateToWidth(s, width));
        }
        terminal.writer().flush();
    }

    private static String truncateToWidth(String line, int width) {
        if (width <= 0 || line == null || line.length() <= width) {
            return line;
        }
        AttributedString attributed = AttributedString.fromAnsi(line);
        if (attributed.columnLength() <= width) {
            return line;
        }
        return attributed.columnSubSequence(0, width).toAnsi();
    }

    private void clean(int lineAmount) {
        if (lineAmount == 0) {
            return;
        }
        terminal.writer().print(String.format("\033[%dA", lineAmount));
        terminal.writer().flush();
        terminal.writer().print(String.format("\033[%dM", lineAmount));
        terminal.writer().flush();
    }

    public synchronized void reset() {
        linesToRemove = 0;
    }
}
