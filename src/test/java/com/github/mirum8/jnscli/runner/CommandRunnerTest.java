package com.github.mirum8.jnscli.runner;

import com.github.mirum8.jnscli.shell.Messages;
import com.github.mirum8.jnscli.shell.RefreshableMultilineRenderer;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.shell.Symbols;
import com.github.mirum8.jnscli.shell.TerminalCapabilities;
import com.github.mirum8.jnscli.shell.TestCapabilities;
import com.github.mirum8.jnscli.shell.Theme;
import org.jline.terminal.Terminal;
import org.jline.terminal.impl.DumbTerminal;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class CommandRunnerTest {

    @Test
    void schedulerKeepsTickingWhenRunningThrows() throws IOException, InterruptedException {
        // given
        CountDownLatch threeTicks = new CountDownLatch(3);
        CountingProgressBar bar = new CountingProgressBar(20, true, threeTicks);
        CommandRunner runner = newRunner();

        // when
        Result<String> actual = runner.call(() -> {
            assertThat(threeTicks.await(2, TimeUnit.SECONDS)).isTrue();
            return "ok";
        }, CommandParameters.<Void>builder().withProgressBar(bar).build());

        // then
        assertThat(actual).isInstanceOf(Result.Success.class);
        assertThat(actual.value()).isEqualTo("ok");
        assertThat(bar.tickCount.get()).isGreaterThanOrEqualTo(3);
    }

    @Test
    void successFrameRendersAfterSchedulerDrained() throws IOException {
        // given
        CountingProgressBar bar = new CountingProgressBar(20, false);
        CommandRunner runner = newRunner();

        // when
        runner.call(() -> "ok", CommandParameters.<Void>builder().withProgressBar(bar).build());

        // then
        assertThat(bar.completedAt).isGreaterThan(0);
        assertThat(bar.lastRunningReturnAt).isLessThanOrEqualTo(bar.completedAt);
    }

    @Test
    void operationResultIsReturnedOnSuccess() throws IOException {
        // given
        CommandRunner runner = newRunner();
        CountingProgressBar bar = new CountingProgressBar(50, false);

        // when
        Result<Integer> actual = runner.call(() -> 42,
            CommandParameters.<Void>builder().withProgressBar(bar).build());

        // then
        assertThat(actual).isInstanceOf(Result.Success.class);
        assertThat(actual.value()).isEqualTo(42);
    }

    private CommandRunner newRunner() throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        Terminal terminal = new DumbTerminal("test", "dumb",
            new ByteArrayInputStream(new byte[0]), out, StandardCharsets.UTF_8);
        TerminalCapabilities caps = TestCapabilities.disabled();
        RefreshableMultilineRenderer renderer = new RefreshableMultilineRenderer(terminal, caps);
        Symbols symbols = new Symbols(caps);
        Messages messages = new Messages(new ShellPrinter(terminal), new Theme(caps), symbols);
        SpinnerFactory factory = new SpinnerFactory(symbols, messages);
        return new CommandRunner(renderer, factory);
    }

    private static final class CountingProgressBar implements ProgressBar {
        private final int refreshMillis;
        private final boolean throwOnRunning;
        private final CountDownLatch tickLatch;
        final AtomicInteger tickCount = new AtomicInteger();
        volatile long lastRunningReturnAt;
        volatile long completedAt;

        CountingProgressBar(int refreshMillis, boolean throwOnRunning) {
            this(refreshMillis, throwOnRunning, new CountDownLatch(0));
        }

        CountingProgressBar(int refreshMillis, boolean throwOnRunning, CountDownLatch tickLatch) {
            this.refreshMillis = refreshMillis;
            this.throwOnRunning = throwOnRunning;
            this.tickLatch = tickLatch;
        }

        @Override
        public List<String> running() {
            tickCount.incrementAndGet();
            tickLatch.countDown();
            if (throwOnRunning) {
                throw new IllegalStateException("simulated transient failure");
            }
            lastRunningReturnAt = System.nanoTime();
            return List.of("running");
        }

        @Override
        public int refreshIntervalMillis() {
            return refreshMillis;
        }

        @Override
        public List<String> completed() {
            completedAt = System.nanoTime();
            return List.of("done");
        }

        @Override
        public List<String> failed() {
            return List.of("fail");
        }
    }
}
