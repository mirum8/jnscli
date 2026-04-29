package com.github.mirum8.jnscli.shell;

public final class TestCapabilities {
    private TestCapabilities() {
    }

    public static TerminalCapabilities of(int width, boolean ansi, boolean unicode) {
        return new TerminalCapabilities(null) {
            @Override
            public int width() {
                return width;
            }

            @Override
            public boolean supportsAnsi() {
                return ansi;
            }

            @Override
            public boolean supportsUnicode() {
                return unicode;
            }
        };
    }

    public static TerminalCapabilities ansi(boolean ansi) {
        return of(80, ansi, ansi);
    }

    public static TerminalCapabilities unicode(boolean unicode) {
        return of(80, unicode, unicode);
    }

    public static TerminalCapabilities disabled() {
        return of(80, false, false);
    }
}
