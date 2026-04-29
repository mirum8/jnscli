package com.github.mirum8.jnscli.runner;

import com.github.mirum8.jnscli.shell.Symbols;
import com.github.mirum8.jnscli.shell.Theme;
import org.springframework.stereotype.Component;

@Component
public class SpinnerFactory {
    private final Theme theme;
    private final Symbols symbols;

    public SpinnerFactory(Theme theme, Symbols symbols) {
        this.theme = theme;
        this.symbols = symbols;
    }

    public Spinner.Builder builder() {
        return new Spinner.Builder(theme, symbols);
    }

    public Spinner.Builder builder(String runningMessage) {
        return new Spinner.Builder(theme, symbols).runningMessage(runningMessage);
    }
}
