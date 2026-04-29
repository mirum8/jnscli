package com.github.mirum8.jnscli.shell;

import org.jline.terminal.Terminal;
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

    public void render(List<String> lines) {
        if (caps.supportsAnsi()) {
            clean(linesToRemove);
            linesToRemove = lines.size();
        }
        for (String s : lines) {
            terminal.writer().println(s);
        }
        terminal.writer().flush();
    }

    private void clean(int lineAmount) {
        if (lineAmount == 0) {
            return;
        }
        terminal.writer().print(String.format("\u001B[%dA", lineAmount));
        terminal.writer().flush();
        terminal.writer().print(String.format("\u001B[%dM", lineAmount));
        terminal.writer().flush();
    }

    public void reset() {
        linesToRemove = 0;
    }
}
