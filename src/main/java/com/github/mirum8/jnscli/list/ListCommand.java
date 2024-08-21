package com.github.mirum8.jnscli.list;

import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class ListCommand {
    private final ListService listService;

    public ListCommand(ListService listService) {
        this.listService = listService;
    }

    @ShellMethod(key = "list", value = "List all jobs on Jenkins server")
    public void list(@ShellOption(defaultValue = ShellOption.NULL, help = "folder name") String folderName) {
        if (folderName != null) {
            listService.listJobs(folderName);
        } else {
            listService.listJobs();
        }
    }

    public void list() {
        list(null);
    }
}
