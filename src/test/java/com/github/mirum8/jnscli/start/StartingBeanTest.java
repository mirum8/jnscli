package com.github.mirum8.jnscli.start;

import com.github.mirum8.jnscli.JshellApplication;
import com.github.mirum8.jnscli.list.ListService;
import com.github.mirum8.jnscli.settings.Settings;
import com.github.mirum8.jnscli.settings.SettingsService;
import com.github.mirum8.jnscli.shell.ShellPrinter;
import com.github.mirum8.jnscli.shell.Theme;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class StartingBeanTest {

    @Test
    void mcpProfileSkipsSettingsAndListing() throws Exception {
        ShellPrinter shellPrinter = mock(ShellPrinter.class);
        SettingsService settingsService = mock(SettingsService.class);
        ListService listService = mock(ListService.class);
        Theme theme = mock(Theme.class);
        Environment environment = mock(Environment.class);
        when(environment.acceptsProfiles(any(Profiles.class))).thenAnswer(inv -> {
            Profiles p = inv.getArgument(0);
            return p.matches(profile -> profile.equals(JshellApplication.MCP_PROFILE));
        });

        StartingBean bean = new StartingBean(
            shellPrinter,
            settingsService,
            listService,
            new DefaultApplicationArguments(),
            theme,
            environment);

        bean.afterPropertiesSet();

        verifyNoInteractions(settingsService);
        verifyNoInteractions(listService);
        verifyNoInteractions(shellPrinter);
    }

    @Test
    void noArgsTriggersJobListingWhenSettingsConfigured() throws Exception {
        ShellPrinter shellPrinter = mock(ShellPrinter.class);
        SettingsService settingsService = mock(SettingsService.class);
        ListService listService = mock(ListService.class);
        Theme theme = mock(Theme.class);
        Environment environment = mock(Environment.class);
        when(environment.acceptsProfiles(any(Profiles.class))).thenReturn(false);
        when(settingsService.readSettings()).thenReturn(new Settings("https://j", "u", "k"));

        StartingBean bean = new StartingBean(
            shellPrinter,
            settingsService,
            listService,
            new DefaultApplicationArguments(),
            theme,
            environment);

        bean.afterPropertiesSet();

        verify(listService).listJobs();
        verify(shellPrinter, never()).println(any());
    }
}
