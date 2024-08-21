package com.github.mirum8.jnscli.jenkins;

import java.util.List;

public record Folder(
    String _class,
    List<Folder.Action> actions,
    String description,
    String displayName,
    String displayNameOrNull,
    String fullDisplayName,
    String fullName,
    String name,
    String url,
    List<Object> healthReport,
    List<Job> jobs,
    Folder.View primaryView,
    List<Folder.View> views
) {

    public record Action(String _class) {
    }

    public record View(String _class, String name, String url) {
    }

}
