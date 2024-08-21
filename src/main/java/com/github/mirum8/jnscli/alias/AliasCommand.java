package com.github.mirum8.jnscli.alias;

import org.springframework.shell.command.annotation.Command;
import org.springframework.stereotype.Component;

@Command(group = "alias", description = "Alias Commands", command = "alias")
@Component
public class AliasCommand {
    private final AliasService aliasService;

    public AliasCommand(AliasService aliasService) {
        this.aliasService = aliasService;
    }

    @Command(command = "add", description = "Add alias")
    public void add(String aliasName, String jobIdOrUrl) {
        aliasService.add(aliasName, jobIdOrUrl);
    }

    @Command(command = "rm", description = "Remove alias")
    public void remove(String aliasName) {
        aliasService.remove(aliasName);
    }

    @Command(command = "ls", description = "List aliases")
    public void list() {
        aliasService.list();
    }
}
