package com.github.mirum8.jnscli.shell;

import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class MessagesTest {

    @Test
    void successTextStartsWithOkGlyphAndMessage() {
        Messages messages = unicode();

        String text = messages.successText("done");

        assertThat(text).contains("✓").contains("done").endsWith("done");
    }

    @Test
    void failureTextStartsWithFailGlyphAndMessage() {
        Messages messages = unicode();

        String text = messages.failureText("oops");

        assertThat(text).contains("✗").contains("oops").endsWith("oops");
    }

    @Test
    void warningTextStartsWithWarnGlyphAndMessage() {
        Messages messages = unicode();

        String text = messages.warningText("careful");

        assertThat(text).contains("!").contains("careful").endsWith("careful");
    }

    @Test
    void infoTextStartsWithInfoGlyphAndMessage() {
        Messages messages = unicode();

        String text = messages.infoText("hello");

        assertThat(text).contains("·").contains("hello").endsWith("hello");
    }

    @Test
    void emptyTextStartsWithEmptyMarkAndMessage() {
        Messages messages = unicode();

        String text = messages.emptyText("nothing here");

        assertThat(text).contains("—").contains("nothing here").endsWith("nothing here");
    }

    @Test
    void asciiSuccessTextHasAsciiGlyph() {
        Messages messages = ascii();

        String text = messages.successText("done");

        assertThat(text).isEqualTo("[OK] done");
    }

    @Test
    void asciiFailureTextHasAsciiGlyph() {
        Messages messages = ascii();

        String text = messages.failureText("oops");

        assertThat(text).isEqualTo("[X] oops");
    }

    @Test
    void asciiEmptyTextHasAsciiDash() {
        Messages messages = ascii();

        String text = messages.emptyText("none");

        assertThat(text).isEqualTo("- none");
    }

    @Test
    void successPrintsLineToTerminal() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DumbTerminal terminal = dumbTerminal(out);
        TerminalCapabilities caps = TestCapabilities.of(80, false, false);
        Messages messages = new Messages(new ShellPrinter(terminal), new Theme(caps), new Symbols(caps));

        messages.success("done");

        assertThat(out.toString(StandardCharsets.UTF_8)).contains("[OK] done");
    }

    @Test
    void failurePrintsLineToTerminal() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DumbTerminal terminal = dumbTerminal(out);
        TerminalCapabilities caps = TestCapabilities.of(80, false, false);
        Messages messages = new Messages(new ShellPrinter(terminal), new Theme(caps), new Symbols(caps));

        messages.failure("nope");

        assertThat(out.toString(StandardCharsets.UTF_8)).contains("[X] nope");
    }

    private static Messages unicode() {
        TerminalCapabilities caps = TestCapabilities.of(80, false, true);
        return new Messages(silentPrinter(), new Theme(caps), new Symbols(caps));
    }

    private static Messages ascii() {
        TerminalCapabilities caps = TestCapabilities.of(80, false, false);
        return new Messages(silentPrinter(), new Theme(caps), new Symbols(caps));
    }

    private static ShellPrinter silentPrinter() {
        try {
            return new ShellPrinter(dumbTerminal(new ByteArrayOutputStream()));
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static DumbTerminal dumbTerminal(ByteArrayOutputStream out) throws IOException {
        return new DumbTerminal("test", "dumb",
            new ByteArrayInputStream(new byte[0]), out, StandardCharsets.UTF_8);
    }
}
