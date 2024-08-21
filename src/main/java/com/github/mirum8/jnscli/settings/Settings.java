package com.github.mirum8.jnscli.settings;

import java.util.HashMap;
import java.util.Map;

public record Settings(
    String server,
    String username,
    String key,
    Map<String, String> aliases
) {
    public Settings(String server, String username, String key) {
        this(server, username, key, Map.of());
    }

    public Settings addAlias(String name, String jobUrl) {
        HashMap<String, String> aliasesMap = new HashMap<>(aliases);
        aliasesMap.put(name, jobUrl);
        return new Settings(server, username, key, Map.copyOf(aliasesMap));
    }

    public Settings removeAlias(String name) {
        HashMap<String, String> aliasesMap = new HashMap<>(aliases);
        aliasesMap.remove(name);
        return new Settings(server, username, key, Map.copyOf(aliasesMap));
    }
}
