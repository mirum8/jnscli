package com.github.mirum8.jnscli.list;

import com.github.mirum8.jnscli.shell.Table;
import com.github.mirum8.jnscli.shell.Theme;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
class JobListTableFormatter {

    private final Table table;
    private final Theme theme;

    JobListTableFormatter(Table table, Theme theme) {
        this.table = table;
        this.theme = theme;
    }

    List<String> createJobTable(List<JobRow> jobs) {
        List<Table.Column> columns = List.of(
            new Table.Column(theme.bold("ID"), Table.Align.RIGHT, new Table.Auto()),
            new Table.Column(theme.bold("St"), Table.Align.LEFT, new Table.Auto()),
            Table.Column.truncate(theme.bold("Name"), 8)
        );
        List<List<String>> rows = jobs.stream()
            .map(j -> List.of(String.valueOf(j.id()), j.status(), j.name()))
            .toList();
        return table.render(columns, rows);
    }
}
