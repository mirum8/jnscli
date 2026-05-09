package com.github.mirum8.jnscli.creds;

import com.github.mirum8.jnscli.settings.Settings;
import com.github.mirum8.jnscli.settings.SettingsProperties;
import com.github.mirum8.jnscli.settings.SettingsService;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CredsFileWriterTest {

    @TempDir
    Path tempDir;

    @Test
    void writesUserPassFileWithExpectedContentAndPath() throws IOException {
        SettingsProperties properties = new SettingsProperties(tempDir.toString());
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.readSettings()).thenReturn(new Settings("https://jenkins.example", "u", "k"));
        CredsFileWriter writer = new CredsFileWriter(properties, settingsService);
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("username", "deploy-bot");
        fields.put("password", "9k!2Lp@xR7vQmA");

        Path written = writer.write("deploy-bot", CredentialType.USER_PASS, fields);

        assertThat(written).hasParentRaw(tempDir.resolve("creds").toAbsolutePath());
        assertThat(written.getFileName().toString())
            .matches(Pattern.compile("deploy-bot-\\d{8}-\\d{6}\\.txt"));
        String content = Files.readString(written);
        assertThat(content)
            .contains("# Server: https://jenkins.example")
            .contains("id: deploy-bot")
            .contains("type: UsernamePassword")
            .contains("username: deploy-bot")
            .contains("password: 9k!2Lp@xR7vQmA");
    }

    @Test
    void writesPosixPermissionsAsOwnerReadWrite() throws IOException {
        Assumptions.assumeTrue(FileSystems.getDefault().supportedFileAttributeViews().contains("posix"));
        SettingsProperties properties = new SettingsProperties(tempDir.toString());
        SettingsService settingsService = mock(SettingsService.class);
        when(settingsService.readSettings()).thenReturn(new Settings("https://jenkins.example", "u", "k"));
        CredsFileWriter writer = new CredsFileWriter(properties, settingsService);

        Path written = writer.write("api-key", CredentialType.SECRET_TEXT, Map.of("secret", "abc"));

        assertThat(Files.getPosixFilePermissions(written))
            .isEqualTo(PosixFilePermissions.fromString("rw-------"));
    }
}
