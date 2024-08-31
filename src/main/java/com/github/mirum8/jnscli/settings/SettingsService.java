package com.github.mirum8.jnscli.settings;

import jakarta.annotation.Nonnull;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import static com.github.mirum8.jnscli.util.FileUtil.resolveHomeDir;

@Component
public class SettingsService {
    public static final String PROPERTIES_SERVER = "server";
    public static final String PROPERTIES_KEY = "key";
    public static final String PROPERTIES_USERNAME = "username";
    public static final String PROPERTIES_ALIASES = "aliases";

    public static final String CONFIG_FILENAME = "config";
    public final String configFilePath;

    private final AiSettingsProvider aiSettingsProvider;

    private Settings settings;

    public SettingsService(SettingsProperties properties, AiSettingsProvider aiSettingsProvider) {
        this.aiSettingsProvider = aiSettingsProvider;
        String configDirectory = resolveHomeDir(properties.directory());
        String fileSeparator = FileSystems.getDefault().getSeparator();
        this.configFilePath = configDirectory + fileSeparator + CONFIG_FILENAME;

        File settingsFile = new File(configDirectory);
        if (!settingsFile.exists()) {
            settingsFile.mkdirs();
        }
        File configFile = new File(configFilePath);

        if (!configFile.exists()) {
            writeSettings(new Settings("", "", "", new HashMap<>(), null));
        }
    }

    @Nonnull
    public Settings readSettings() {
        if (settings != null) {
            return settings;
        }
        Properties prop = new Properties();
        try (InputStream inputStream = new FileInputStream(configFilePath)) {
            prop.load(inputStream);
        } catch (IOException e) {
            throw new IllegalStateException("Error reading settings file", e);
        }

        String aliasesString = prop.getProperty(PROPERTIES_ALIASES, "");
        Map<String, String> aliases = parseAliasesString(aliasesString);

        return new Settings(
            prop.getProperty(PROPERTIES_SERVER, ""),
            prop.getProperty(PROPERTIES_USERNAME, ""),
            prop.getProperty(PROPERTIES_KEY, ""),
            aliases,
            aiSettingsProvider.getFromProps(prop).orElse(null)
        );
    }

    public void writeSettings(Settings settings) {
        try (OutputStream outputStream = new FileOutputStream(configFilePath)) {
            Properties prop = new Properties();
            prop.setProperty(PROPERTIES_SERVER, settings.server());
            prop.setProperty(PROPERTIES_KEY, settings.key());
            prop.setProperty(PROPERTIES_USERNAME, settings.username());

            String aliasesString = settings.aliases().entrySet()
                .stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(","));
            prop.setProperty(PROPERTIES_ALIASES, aliasesString);
            if (settings.aiSettings() != null) {
                settings.aiSettings().writeToProps(prop);
            }
            prop.store(outputStream, null);
            this.settings = settings;
        } catch (IOException e) {
            throw new IllegalStateException("Error writing to settings file", e);
        }
    }

    private Map<String, String> parseAliasesString(String aliasesString) {
        Map<String, String> aliases = new HashMap<>();
        if (!aliasesString.isEmpty()) {
            String[] aliasPairs = aliasesString.split(",");
            for (String pair : aliasPairs) {
                String[] entry = pair.split("=");
                if (entry.length == 2) {
                    aliases.put(entry[0], entry[1]);
                }
            }
        }
        return aliases;
    }

    public void writeAiSettings(AiSettings aiSettings) {
        Settings updatedSettings = readSettings();
        writeSettings(updatedSettings.updateAiSettings(aiSettings));
    }

    public Optional<AiSettings> readAiSettings() {
        return Optional.ofNullable(readSettings().aiSettings());
    }

}
