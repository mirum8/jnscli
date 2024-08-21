package com.github.mirum8.jnscli.shell;

import org.jline.terminal.Terminal;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Component responsible for rendering output with the ability to refresh the displayed content.
 * Not thread-safe.
 */
@Component
public class RefreshableMultilineRenderer {
    private int linesToRemove;
    private final Terminal terminal;

    /**
     * Constructor for RefreshableOutputRenderer.
     *
     * @param terminal Terminal instance used for output rendering.
     */
    public RefreshableMultilineRenderer(@Lazy Terminal terminal) {
        this.terminal = terminal;
    }

    public void render(String line) {
        var lines = line.split("\n");
        render(List.of(lines));
    }

    public void render(List<String> lines) {
        clean(linesToRemove);
        linesToRemove = lines.size();
        for (String s : lines) {
            terminal.writer().println(s);
        }
        terminal.writer().flush();
    }

    private void clean(int lineAmount) {
        if (lineAmount == 0) {
            return;
        }
        // Move the cursor up lineAmount lines
        terminal.writer().print(String.format("\u001B[%dA", lineAmount));
        terminal.writer().flush();

        // Delete lineAmount lines
        terminal.writer().print(String.format("\u001B[%dM", lineAmount));
        terminal.writer().flush();
    }

    /**
     * Resets the number of lines to remove to zero.
     */
    public void reset() {
        linesToRemove = 0;
    }
}
