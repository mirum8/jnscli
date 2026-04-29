package com.github.mirum8.jnscli.shell;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TableTest {

    @Test
    void displayWidthStripsAnsiEscapes() {
        String coloredFoo = "[32mfoo[0m";

        int actualWidth = Table.displayWidth(coloredFoo);

        assertThat(actualWidth).isEqualTo(3);
    }

    @Test
    void displayWidthReturnsZeroForNull() {
        int actualWidth = Table.displayWidth(null);

        assertThat(actualWidth).isZero();
    }

    @Test
    void rowsAlignWhenContentMixesColoredAndPlain() {
        Table table = new Table(capsWith(120, true, true));
        List<Table.Column> columns = List.of(
            Table.Column.of("ID"),
            Table.Column.of("Status"),
            Table.Column.of("Name")
        );
        List<List<String>> rows = List.of(
            List.of("1", "[32m✓[0m", "alpha"),
            List.of("22", "✗", "beta")
        );

        List<String> rendered = table.render(columns, rows);

        int firstRowVisibleWidth = Table.displayWidth(rendered.get(1));
        int secondRowVisibleWidth = Table.displayWidth(rendered.get(2));
        assertThat(firstRowVisibleWidth).isEqualTo(secondRowVisibleWidth);
    }

    @Test
    void truncateColumnAddsUnicodeEllipsisWhenContentTooLong() {
        Table table = new Table(capsWith(15, true, true));
        List<Table.Column> columns = List.of(
            Table.Column.of("ID"),
            Table.Column.truncate("Name", 4)
        );
        List<List<String>> rows = List.of(
            List.of("1", "averyverylongnamehere")
        );

        List<String> rendered = table.render(columns, rows);

        assertThat(rendered.get(1)).contains("…");
    }

    @Test
    void truncateUsesAsciiEllipsisWhenUnicodeUnsupported() {
        Table table = new Table(capsWith(15, true, false));
        List<Table.Column> columns = List.of(
            Table.Column.of("ID"),
            Table.Column.truncate("Name", 4)
        );
        List<List<String>> rows = List.of(
            List.of("1", "averyverylongnamehere")
        );

        List<String> rendered = table.render(columns, rows);

        assertThat(rendered.get(1)).contains("...");
    }

    @Test
    void noTruncationWhenTerminalIsWideEnough() {
        Table table = new Table(capsWith(120, true, true));
        List<Table.Column> columns = List.of(
            Table.Column.of("ID"),
            Table.Column.truncate("Name", 4)
        );
        List<List<String>> rows = List.of(
            List.of("1", "averyverylongnamehere")
        );

        List<String> rendered = table.render(columns, rows);

        assertThat(rendered.get(1)).contains("averyverylongnamehere").doesNotContain("…");
    }

    private TerminalCapabilities capsWith(int w, boolean ansi, boolean unicode) {
        return TestCapabilities.of(w, ansi, unicode);
    }
}
