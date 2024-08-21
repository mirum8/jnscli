package com.github.mirum8.jnscli.alias;

import com.github.mirum8.jnscli.context.JobsContext;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.settings.Settings;
import com.github.mirum8.jnscli.settings.SettingsService;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.util.URLValidator;
import org.springframework.stereotype.Component;

import java.util.Optional;

import static com.github.mirum8.jnscli.util.Strings.isJobNumber;

@Component
public class AliasService {
    private final SettingsService settingsService;
    private final JobsContext jobsContext;
    private final ShellPrinter shellPrinter;

    public AliasService(SettingsService settingsService, JobsContext jobsContext, ShellPrinter shellPrinter) {
        this.settingsService = settingsService;
        this.jobsContext = jobsContext;
        this.shellPrinter = shellPrinter;
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
        settingsService.readSettings().aliases().forEach((alias, url) -> shellPrinter.println(alias + " -> " + url));
    }
}
