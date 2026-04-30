package com.github.mirum8.jnscli.build;

import com.github.mirum8.jnscli.shell.Symbols;
import com.github.mirum8.jnscli.shell.TerminalCapabilities;
import com.github.mirum8.jnscli.shell.TestCapabilities;
import com.github.mirum8.jnscli.shell.Theme;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PercentageBarTest {

    @Test
    void unicodeRunningBarHasIconBlocksAndPercent() {
        PercentageBar bar = unicodeBar(80);

        String line = bar.of(50, "compile");

        assertThat(line).startsWith("● ");
        assertThat(line).contains("50%");
        assertThat(line).endsWith("compile");
        assertThat(line).contains("█");
        assertThat(line).contains("░");
    }

    @Test
    void unicodeDoneBarUsesCheckIcon() {
        PercentageBar bar = unicodeBar(80);

        String line = bar.of(100, "lint");

        assertThat(line).startsWith("✓ ");
        assertThat(line).contains("100%");
        assertThat(line).endsWith("lint");
        assertThat(line).doesNotContain("░");
    }

    @Test
    void unicodeErrorBarUsesCrossIcon() {
        PercentageBar bar = unicodeBar(80);

        String line = bar.error(85, "deploy");

        assertThat(line).startsWith("✗ ");
        assertThat(line).contains("85%");
        assertThat(line).endsWith("deploy");
    }

    @Test
    void unicodeZeroPercentHasNoFilledBlocks() {
        PercentageBar bar = unicodeBar(80);

        String line = bar.of(0, "init");

        assertThat(line).contains("0%");
        assertThat(line).doesNotContain("█");
        assertThat(line).contains("░");
    }

    @Test
    void unicodeSubCellPartialBlocksAppearAtFractionalPercents() {
        PercentageBar bar = unicodeBar(80);

        boolean foundPartial = false;
        for (int pct : new int[]{7, 33, 67, 88}) {
            String line = bar.of(pct, "x");
            if (line.chars().anyMatch(c -> "▏▎▍▌▋▊▉".indexOf(c) >= 0)) {
                foundPartial = true;
                break;
            }
        }
        assertThat(foundPartial).isTrue();
    }

    @Test
    void asciiFallbackHasBracketsPercentAndLabelButNoIcon() {
        PercentageBar bar = asciiBar(80);

        String line = bar.of(60, "compile");

        assertThat(line).startsWith("[");
        assertThat(line).contains("] ");
        assertThat(line).contains("60%");
        assertThat(line).endsWith("compile");
        assertThat(line).doesNotContain("●");
        assertThat(line).doesNotContain("▎");
    }

    @Test
    void asciiHundredPercentHasFullBarAndLabel() {
        PercentageBar bar = asciiBar(80);

        String line = bar.of(100, "lint");

        assertThat(line).contains("100%");
        assertThat(line).contains("lint");
        assertThat(line).contains("=");
    }

    @Test
    void barWidthClampsToMinAndMax() {
        String narrow = unicodeBar(20).of(50, "x");
        String wide = unicodeBar(200).of(50, "x");

        long narrowBlocks = countAny(narrow, "█▏▎▍▌▋▊▉░");
        long wideBlocks = countAny(wide, "█▏▎▍▌▋▊▉░");

        assertThat(narrowBlocks).isEqualTo(8);
        assertThat(wideBlocks).isEqualTo(24);
    }

    @Test
    void doneIconUnicodeIsCheck() {
        assertThat(unicodeBar(80).doneIcon()).isEqualTo("✓");
    }

    @Test
    void doneIconAsciiIsEmpty() {
        assertThat(asciiBar(80).doneIcon()).isEmpty();
    }

    private static PercentageBar unicodeBar(int width) {
        TerminalCapabilities caps = TestCapabilities.of(width, false, true);
        return new PercentageBar(caps, new Theme(caps), new Symbols(caps));
    }

    private static PercentageBar asciiBar(int width) {
        TerminalCapabilities caps = TestCapabilities.of(width, false, false);
        return new PercentageBar(caps, new Theme(caps), new Symbols(caps));
    }

    private static long countAny(String s, String chars) {
        return s.chars().filter(c -> chars.indexOf(c) >= 0).count();
    }
}
