package com.github.mirum8.jnscli.start;

import com.github.mirum8.jnscli.list.ListService;
import com.github.mirum8.jnscli.settings.Settings;
import com.github.mirum8.jnscli.settings.SettingsService;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.github.mirum8.jnscli.shell.TextColor.RED;
import static com.github.mirum8.jnscli.shell.TextFormatter.colored;

@Component
public class StartingBean implements InitializingBean {
    private final ShellPrinter shellPrinter;
    private final SettingsService settingsService;
    private final ListService listService;
    private final ApplicationArguments applicationArguments;

    public StartingBean(ShellPrinter shellPrinter,
                        SettingsService settingsService,
                        ListService listService,
                        ApplicationArguments applicationArguments) {
        this.shellPrinter = shellPrinter;
        this.settingsService = settingsService;
        this.listService = listService;
        this.applicationArguments = applicationArguments;
    }

    @Override
    public void afterPropertiesSet() {
        Settings settings = settingsService.readSettings();
        if (settings.server().isEmpty() || settings.username().isEmpty() || settings.key().isEmpty()) {
            shellPrinter.println(colored("Please configure your settings first", RED));
            shellPrinter.println(colored("Run 'connect' command to configure your settings", RED));
        } else {
            List<String> nonOptionArgs = applicationArguments.getNonOptionArgs();
            if (nonOptionArgs.isEmpty()) {
                listService.listJobs();
            }
        }
    }


}
