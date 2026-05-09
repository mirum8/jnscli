package com.github.mirum8.jnscli.creds;

import com.github.mirum8.jnscli.settings.SettingsProperties;
import com.github.mirum8.jnscli.settings.SettingsService;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

import static com.github.mirum8.jnscli.util.FileUtil.resolveHomeDir;

@Component
public class CredsFileWriter {
    private static final DateTimeFormatter FILE_TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");
    private static final DateTimeFormatter HEADER_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final SettingsProperties settingsProperties;
    private final SettingsService settingsService;

    public CredsFileWriter(SettingsProperties settingsProperties, SettingsService settingsService) {
        this.settingsProperties = settingsProperties;
        this.settingsService = settingsService;
    }

    public Path write(String credId, CredentialType type, Map<String, String> fields) {
        CredentialIds.validate(credId);
        LocalDateTime now = LocalDateTime.now();
        Path dir = Path.of(resolveHomeDir(settingsProperties.directory()), "creds");
        Path file = dir.resolve(credId + "-" + FILE_TIMESTAMP.format(now) + ".txt");
        try {
            Files.createDirectories(dir);
            Files.writeString(file, render(credId, type, fields, now), StandardCharsets.UTF_8);
            applyPosixPermissions(file);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write credentials file: " + file, e);
        }
        return file.toAbsolutePath();
    }

    private String render(String credId, CredentialType type, Map<String, String> fields, LocalDateTime now) {
        StringBuilder sb = new StringBuilder();
        sb.append("# Jenkins credentials — generated ").append(HEADER_TIMESTAMP.format(now)).append('\n');
        sb.append("# Server: ").append(settingsService.readSettings().server()).append('\n');
        sb.append("id: ").append(credId).append('\n');
        sb.append("type: ").append(type.displayName()).append('\n');
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            sb.append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
        return sb.toString();
    }

    private static void applyPosixPermissions(Path file) throws IOException {
        if (FileSystems.getDefault().supportedFileAttributeViews().contains("posix")) {
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
        }
    }
}
