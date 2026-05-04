package com.github.mirum8.jnscli.shell;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutputContextTest {

    @Test
    void explicitJsonOverridesEnvDetection() {
        OutputContext ctx = new OutputContext("json", Map.of("CLAUDECODE", "1"));
        assertThat(ctx.mode()).isEqualTo(OutputMode.JSON);
        assertThat(ctx.isJson()).isTrue();
    }

    @Test
    void claudeCodeEnvSelectsPlain() {
        OutputContext ctx = new OutputContext(null, Map.of("CLAUDECODE", "1"));
        assertThat(ctx.mode()).isEqualTo(OutputMode.PLAIN);
    }

    @Test
    void ciEnvSelectsPlain() {
        OutputContext ctx = new OutputContext(null, Map.of("CI", "true"));
        assertThat(ctx.mode()).isEqualTo(OutputMode.PLAIN);
    }

    @Test
    void blankEnvFallsBackToConsoleProbe() {
        OutputContext ctx = new OutputContext(null, Map.of());
        assertThat(ctx.mode()).isIn(OutputMode.PLAIN, OutputMode.RICH);
    }

    @Test
    void unknownExplicitModeThrows() {
        assertThatThrownBy(() -> new OutputContext("verbose", Map.of()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Unknown output mode");
    }
}
