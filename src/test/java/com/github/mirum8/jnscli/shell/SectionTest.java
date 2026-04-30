package com.github.mirum8.jnscli.shell;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SectionTest {

    @Test
    void fieldsAlignColonsToLongestLabel() {
        Section section = section();

        String output = section.builder()
            .field("A", "1")
            .field("Long Label", "2")
            .field("Mid", "3")
            .build();

        String[] lines = output.split("\n");
        assertThat(lines[0]).contains("A         :");
        assertThat(lines[1]).contains("Long Label:");
        assertThat(lines[2]).contains("Mid       :");
    }

    @Test
    void singleFieldHasNoExtraPadding() {
        Section section = section();

        String output = section.builder().field("X", "y").build();

        assertThat(output).isEqualTo("  X: y\n");
    }

    @Test
    void headerRendersBeforeFields() {
        Section section = section();

        String output = section.builder()
            .header("Title")
            .field("K", "v")
            .build();

        String[] lines = output.split("\n");
        assertThat(lines[0]).isEqualTo("Title");
        assertThat(lines[1]).isEqualTo("  K: v");
    }

    @Test
    void dividerRendersAsDashesAtAnsiSupport() {
        TerminalCapabilities caps = TestCapabilities.of(40, true, false);
        Section section = new Section(new Theme(caps), caps);

        String output = section.builder().divider().build();

        assertThat(output).contains("-".repeat(40));
    }

    @Test
    void dividerWithoutAnsiIsBlankLine() {
        Section section = section();

        String output = section.builder().divider().build();

        assertThat(output).isEqualTo("\n");
    }

    @Test
    void blankAddsEmptyLine() {
        Section section = section();

        String output = section.builder()
            .field("a", "1")
            .blank()
            .field("b", "2")
            .build();

        String[] lines = output.split("\n", -1);
        assertThat(lines[0]).isEqualTo("  a: 1");
        assertThat(lines[1]).isEmpty();
        assertThat(lines[2]).isEqualTo("  b: 2");
    }

    @Test
    void lineEmittedVerbatim() {
        Section section = section();

        String output = section.builder().line("raw text").build();

        assertThat(output).isEqualTo("raw text\n");
    }

    private static Section section() {
        TerminalCapabilities caps = TestCapabilities.disabled();
        return new Section(new Theme(caps), caps);
    }
}
