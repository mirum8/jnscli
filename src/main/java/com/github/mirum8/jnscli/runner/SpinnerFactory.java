package com.github.mirum8.jnscli.runner;

import com.github.mirum8.jnscli.shell.Messages;
import com.github.mirum8.jnscli.shell.Symbols;
import org.springframework.stereotype.Component;

@Component
public class SpinnerFactory {
    private final Symbols symbols;
    private final Messages messages;

    public SpinnerFactory(Symbols symbols, Messages messages) {
        this.symbols = symbols;
        this.messages = messages;
    }

    public Spinner.Builder builder() {
        return new Spinner.Builder(symbols, messages);
    }

    public Spinner.Builder builder(String runningMessage) {
        return new Spinner.Builder(symbols, messages).runningMessage(runningMessage);
    }
}
