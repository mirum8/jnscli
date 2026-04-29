package com.github.mirum8.jnscli.shell;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ThemeTest {

    @Test
    void labelReturnsRawStringWhenAnsiUnsupported() {
        Theme theme = new Theme(capsWith(false));

        String actual = theme.label("Status: ");

        assertThat(actual).isEqualTo("Status: ");
    }

    @Test
    void labelWrapsStringInAnsiWhenAnsiSupported() {
        Theme theme = new Theme(capsWith(true));

        String actual = theme.label("Status: ");

        assertThat(actual).contains("[").contains("Status: ");
    }

    @Test
    void successReturnsRawStringWhenAnsiUnsupported() {
        Theme theme = new Theme(capsWith(false));

        String actual = theme.success("ok");

        assertThat(actual).isEqualTo("ok");
    }

    @Test
    void boldReturnsRawStringWhenAnsiUnsupported() {
        Theme theme = new Theme(capsWith(false));

        String actual = theme.bold("HEAD");

        assertThat(actual).isEqualTo("HEAD");
    }

    @Test
    void failureWrapsStringInAnsiWhenAnsiSupported() {
        Theme theme = new Theme(capsWith(true));

        String actual = theme.failure("oops");

        assertThat(actual).contains("[").contains("oops");
    }

    @Test
    void nullInputReturnsNull() {
        Theme theme = new Theme(capsWith(true));

        String actual = theme.success(null);

        assertThat(actual).isNull();
    }

    private TerminalCapabilities capsWith(boolean ansi) {
        return TestCapabilities.ansi(ansi);
    }
}
