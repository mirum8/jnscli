package com.github.mirum8.jnscli.shell;

import org.springframework.stereotype.Component;

@Component
public class Messages {
    private final ShellPrinter printer;
    private final Theme theme;
    private final Symbols symbols;

    public Messages(ShellPrinter printer, Theme theme, Symbols symbols) {
        this.printer = printer;
        this.theme = theme;
        this.symbols = symbols;
    }

    public void success(String message) {
        printer.println(successText(message));
    }

    public void failure(String message) {
        printer.println(failureText(message));
    }

    public void warning(String message) {
        printer.println(warningText(message));
    }

    public void info(String message) {
        printer.println(infoText(message));
    }

    public void empty(String message) {
        printer.println(emptyText(message));
    }

    public String successText(String message) {
        return theme.success(symbols.ok()) + " " + message;
    }

    public String failureText(String message) {
        return theme.failure(symbols.fail()) + " " + message;
    }

    public String warningText(String message) {
        return theme.warning(symbols.warn()) + " " + message;
    }

    public String infoText(String message) {
        return theme.dim(symbols.info()) + " " + message;
    }

    public String emptyText(String message) {
        return theme.dim(symbols.emptyMark() + " " + message);
    }
}
