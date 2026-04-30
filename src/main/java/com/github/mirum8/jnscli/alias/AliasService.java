package com.github.mirum8.jnscli.alias;

import com.github.mirum8.jnscli.context.JobsContext;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.settings.Settings;
import com.github.mirum8.jnscli.settings.SettingsService;
import com.github.mirum8.jnscli.shell.Messages;
import com.github.mirum8.jnscli.shell.RefreshableMultilineRenderer;
import com.github.mirum8.jnscli.shell.Table;
import com.github.mirum8.jnscli.shell.Theme;
import com.github.mirum8.jnscli.util.URLValidator;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.github.mirum8.jnscli.util.Strings.isJobNumber;

@Component
public class AliasService {
    private final SettingsService settingsService;
    private final JobsContext jobsContext;
    private final Table table;
    private final Theme theme;
    private final RefreshableMultilineRenderer renderer;
    private final Messages messages;

    public AliasService(SettingsService settingsService, JobsContext jobsContext, Table table, Theme theme, RefreshableMultilineRenderer renderer, Messages messages) {
        this.settingsService = settingsService;
        this.jobsContext = jobsContext;
        this.table = table;
        this.theme = theme;
        this.renderer = renderer;
        this.messages = messages;
    }

    void add(String alias, String jobIdOrUrl) {
        if (isJobNumber(jobIdOrUrl)) {
            add(alias, Integer.parseInt(jobIdOrUrl.substring(1)));
            return;
        }
        URLValidator.check(jobIdOrUrl);
        Settings settings = settingsService.readSettings();
        settingsService.writeSettings(settings.addAlias(alias, jobIdOrUrl));
    }

    private void add(String alias, int id) {
        jobsContext.findJobById(id)
            .map(JobDescriptor::url)
            .ifPresentOrElse(url -> add(alias, url), () -> {
                throw new IllegalArgumentException("Job with id " + id + " not found");
            });
    }

    public Optional<String> getJobUrl(String jobName) {
        return Optional.ofNullable(settingsService.readSettings().aliases().get(jobName));
    }

    void remove(String aliasName) {
        Settings settings = settingsService.readSettings();
        settingsService.writeSettings(settings.removeAlias(aliasName));
    }

    void list() {
        Map<String, String> aliases = settingsService.readSettings().aliases();
        if (aliases.isEmpty()) {
            messages.empty("No aliases configured.");
            return;
        }
        List<Table.Column> columns = List.of(
            new Table.Column(theme.bold("Alias"), Table.Align.LEFT, new Table.Auto()),
            Table.Column.truncate(theme.bold("URL"), 20)
        );
        List<List<String>> rows = aliases.entrySet().stream()
            .map(e -> List.of(e.getKey(), e.getValue()))
            .toList();
        renderer.render(table.render(columns, rows));
    }
}
