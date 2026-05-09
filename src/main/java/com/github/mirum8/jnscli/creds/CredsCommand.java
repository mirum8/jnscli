package com.github.mirum8.jnscli.creds;

import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;
import org.springframework.stereotype.Component;

@Command(group = "creds", description = "Credentials Commands", command = "creds")
@Component
public class CredsCommand {
    private final CredsService credsService;

    public CredsCommand(CredsService credsService) {
        this.credsService = credsService;
    }

    @Command(command = "ls", description = "List Jenkins credentials")
    public void list() {
        credsService.list();
    }

    @Command(command = "rm", description = "Remove a Jenkins credential")
    public void remove(String id) {
        credsService.remove(id);
    }

    @Command(command = "add", description = "Add a Jenkins credential")
    public void add(String id,
                    @Option(longNames = "type", shortNames = 't', description = "user-pass or secret-text") String type,
                    @Option(longNames = "username", shortNames = 'u') String username,
                    @Option(longNames = "password") String password,
                    @Option(longNames = "secret") String secret,
                    @Option(longNames = "random", shortNames = 'r', description = "Generate a random password/secret") boolean random,
                    @Option(longNames = "description", shortNames = 'd') String description,
                    @Option(longNames = "scope", description = "GLOBAL or SYSTEM (default GLOBAL)") String scope) {
        credsService.add(id, type, username, password, secret, random, description, scope);
    }
}
