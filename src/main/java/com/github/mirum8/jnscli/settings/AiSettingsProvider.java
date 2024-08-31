package com.github.mirum8.jnscli.settings;

import java.util.Optional;
import java.util.Properties;

public interface AiSettingsProvider {
    Optional<AiSettings> getFromProps(Properties props);
}
