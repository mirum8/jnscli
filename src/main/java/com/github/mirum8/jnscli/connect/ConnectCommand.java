package com.github.mirum8.jnscli.connect;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;

@ShellComponent
public class ConnectCommand {
    private final ConnectService connectService;

    public ConnectCommand(ConnectService connectService) {
        this.connectService = connectService;
    }

    @ShellMethod("Connect to Jenkins server")
    public void connect() {
       connectService.connect();
    }
}
