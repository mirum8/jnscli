package com.github.mirum8.jnscli.shell;

import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RefreshableMultilineRendererTest {

    @Test
    void emitsNoCsiEscapesWhenAnsiUnsupported() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal terminal = dumbTerminal(out);
        RefreshableMultilineRenderer renderer = new RefreshableMultilineRenderer(terminal, capsWith(false));

        renderer.render(List.of("first"));
        renderer.render(List.of("second"));

        assertThat(out.toString(StandardCharsets.UTF_8))
            .doesNotContain("[")
            .contains("first")
            .contains("second");
    }

    @Test
    void emitsCursorEscapesOnSubsequentRenderWhenAnsiSupported() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal terminal = dumbTerminal(out);
        RefreshableMultilineRenderer renderer = new RefreshableMultilineRenderer(terminal, capsWith(true));

        renderer.render(List.of("first"));
        renderer.render(List.of("second"));

        assertThat(out.toString(StandardCharsets.UTF_8))
            .contains("[1A")
            .contains("[1M");
    }

    @Test
    void resetClearsLineCountSoNextRenderEmitsNoEscapes() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal terminal = dumbTerminal(out);
        RefreshableMultilineRenderer renderer = new RefreshableMultilineRenderer(terminal, capsWith(true));

        renderer.render(List.of("first"));
        renderer.reset();
        out.reset();
        renderer.render(List.of("second"));

        String written = out.toString(StandardCharsets.UTF_8);
        assertThat(written).doesNotContain("[1A").contains("second");
    }

    @Test
    void truncatesLinesWiderThanTerminalWidth() throws IOException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal terminal = dumbTerminal(out);
        RefreshableMultilineRenderer renderer = new RefreshableMultilineRenderer(terminal, TestCapabilities.of(10, true, true));

        // when
        renderer.render(List.of("0123456789ABCDEFGHIJ"));

        // then
        String written = out.toString(StandardCharsets.UTF_8);
        assertThat(written).contains("0123456789").doesNotContain("ABCDEFGHIJ");
    }

    @Test
    void preservesAnsiColorWhenTruncating() throws IOException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal terminal = dumbTerminal(out);
        RefreshableMultilineRenderer renderer = new RefreshableMultilineRenderer(terminal, TestCapabilities.of(5, true, true));
        String coloredLong = "[31m0123456789[0m";

        // when
        renderer.render(List.of(coloredLong));

        // then
        String written = out.toString(StandardCharsets.UTF_8);
        assertThat(written).contains("[31m").contains("01234").doesNotContain("56789");
    }

    @Test
    void leavesShortLinesUntouched() throws IOException {
        // given
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal terminal = dumbTerminal(out);
        RefreshableMultilineRenderer renderer = new RefreshableMultilineRenderer(terminal, TestCapabilities.of(80, true, true));

        // when
        renderer.render(List.of("hello"));

        // then
        assertThat(out.toString(StandardCharsets.UTF_8)).contains("hello");
    }

    private DumbTerminal dumbTerminal(ByteArrayOutputStream out) throws IOException {
        return new DumbTerminal("test", "dumb",
            new ByteArrayInputStream(new byte[0]), out, StandardCharsets.UTF_8);
    }

    private TerminalCapabilities capsWith(boolean ansi) {
        return TestCapabilities.ansi(ansi);
    }
}
