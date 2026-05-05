package com.github.mirum8.jnscli.start;

import com.github.mirum8.jnscli.JshellApplication;
import com.github.mirum8.jnscli.list.ListService;
import com.github.mirum8.jnscli.settings.Settings;
import com.github.mirum8.jnscli.settings.SettingsService;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.shell.Theme;
import com.github.mirum8.jnscli.util.FileUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.ApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

@Component
public class StartingBean implements InitializingBean {
    private final ShellPrinter shellPrinter;
    private final SettingsService settingsService;
    private final ListService listService;
    private final ApplicationArguments applicationArguments;
    private final Theme theme;
    private final Environment environment;

    public StartingBean(ShellPrinter shellPrinter,
                        SettingsService settingsService,
                        ListService listService,
                        ApplicationArguments applicationArguments,
                        Theme theme,
                        Environment environment) {
        this.shellPrinter = shellPrinter;
        this.settingsService = settingsService;
        this.listService = listService;
        this.applicationArguments = applicationArguments;
        this.theme = theme;
        this.environment = environment;
    }

    @Override
    public void afterPropertiesSet() throws IOException {
        setupLogger();

        if (environment.acceptsProfiles(Profiles.of(JshellApplication.MCP_PROFILE))) {
            return;
        }

        Settings settings = settingsService.readSettings();
        if (settings.server().isEmpty() || settings.username().isEmpty() || settings.key().isEmpty()) {
            shellPrinter.println(theme.failure("Please configure your settings first"));
            shellPrinter.println(theme.failure("Run 'connect' command to configure your settings"));
        } else {
            List<String> nonOptionArgs = applicationArguments.getNonOptionArgs();
            if (nonOptionArgs.isEmpty()) {
                listService.listJobs();
            }
        }
    }

    private static void setupLogger() throws IOException {
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(Level.WARNING);
        FileHandler handler = new FileHandler(FileUtil.resolveHomeDir("~/.config/jns/error.log"), true);
        handler.setFormatter(new SimpleFormatter());
        rootLogger.addHandler(handler);
    }


}
