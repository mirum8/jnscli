package com.github.mirum8.jnscli.list;

import com.github.mirum8.jnscli.shell.Table;
import com.github.mirum8.jnscli.shell.TerminalCapabilities;
import com.github.mirum8.jnscli.shell.TestCapabilities;
import com.github.mirum8.jnscli.shell.Theme;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JobListTableFormatterTest {

    @Test
    void rowsAlignWhenStatusesAreColoredAndFolderNameIsBold() {
        TerminalCapabilities caps = capsWith(120, true, true);
        Theme theme = new Theme(caps);
        Table table = new Table(caps);
        JobListTableFormatter formatter = new JobListTableFormatter(table, theme);
        List<JobRow> jobs = List.of(
            new JobRow(1, theme.success("✓"), "alpha"),
            new JobRow(2, theme.failure("✗"), "beta"),
            new JobRow(3, "", theme.bold("folder"))
        );

        List<String> rows = formatter.createJobTable(jobs);

        int row1Width = Table.displayWidth(rows.get(1));
        int row2Width = Table.displayWidth(rows.get(2));
        int row3Width = Table.displayWidth(rows.get(3));
        assertThat(row1Width).isEqualTo(row2Width);
        assertThat(row2Width).isEqualTo(row3Width);
    }

    @Test
    void firstLineIsHeaderRowContainingColumnTitles() {
        TerminalCapabilities caps = capsWith(120, true, true);
        Theme theme = new Theme(caps);
        Table table = new Table(caps);
        JobListTableFormatter formatter = new JobListTableFormatter(table, theme);
        List<JobRow> jobs = List.of(new JobRow(1, "✓", "alpha"));

        List<String> rows = formatter.createJobTable(jobs);

        assertThat(rows.get(0)).contains("ID").contains("St").contains("Name");
    }

    private TerminalCapabilities capsWith(int w, boolean ansi, boolean unicode) {
        return TestCapabilities.of(w, ansi, unicode);
    }
}
