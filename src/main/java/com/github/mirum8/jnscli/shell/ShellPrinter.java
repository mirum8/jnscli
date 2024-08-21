package com.github.mirum8.jnscli.shell;

import org.jline.terminal.Terminal;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

@Component
public class ShellPrinter {

    private final Terminal terminal;

    public ShellPrinter(@Lazy Terminal terminal) {
        this.terminal = terminal;
    }

    public void println() {
        terminal.writer().println();
        terminal.flush();
    }

    public void println(String message) {
        terminal.writer().println(message);
        terminal.flush();
    }

    public void print(String message) {
        terminal.writer().print(message);
        terminal.flush();
    }
}
