package com.github.mirum8.jnscli.shell;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SymbolsTest {

    @Test
    void unicodeOkGlyph() {
        Symbols symbols = new Symbols(capsUnicode(true));

        assertThat(symbols.ok()).isEqualTo("✓");
    }

    @Test
    void asciiOkGlyph() {
        Symbols symbols = new Symbols(capsUnicode(false));

        assertThat(symbols.ok()).isEqualTo("[OK]");
    }

    @Test
    void asciiFailGlyph() {
        Symbols symbols = new Symbols(capsUnicode(false));

        assertThat(symbols.fail()).isEqualTo("[X]");
    }

    @Test
    void unicodeFolderGlyph() {
        Symbols symbols = new Symbols(capsUnicode(true));

        assertThat(symbols.folder()).isEqualTo("▸");
    }

    @Test
    void brailleSpinnerFramesWhenUnicodeSupported() {
        Symbols symbols = new Symbols(capsUnicode(true));

        char[] frames = symbols.spinnerFrames();

        assertThat(frames).hasSize(10);
        assertThat(frames[0]).isEqualTo('⠋');
    }

    @Test
    void asciiSpinnerFramesWhenUnicodeUnsupported() {
        Symbols symbols = new Symbols(capsUnicode(false));

        char[] frames = symbols.spinnerFrames();

        assertThat(frames).containsExactly('|', '/', '-', '\\');
    }

    private TerminalCapabilities capsUnicode(boolean unicode) {
        return TestCapabilities.unicode(unicode);
    }
}
