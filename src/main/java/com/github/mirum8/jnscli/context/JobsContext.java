package com.github.mirum8.jnscli.context;

import com.github.mirum8.jnscli.jenkins.Job;
import com.github.mirum8.jnscli.model.JobDescriptor;
import com.github.mirum8.jnscli.settings.SettingsProperties;
import com.github.mirum8.jnscli.util.FileUtil;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static java.nio.file.StandardOpenOption.APPEND;

@Component
public class JobsContext {
    public static final String MAPPING_FILENAME = "mapping";
    private final Path mappingFilePath;

    public JobsContext(SettingsProperties settingsProperties) {
        String configDirectory = FileUtil.resolveHomeDir(settingsProperties.directory());
        this.mappingFilePath = Paths.get(configDirectory + FileSystems.getDefault().getSeparator() + MAPPING_FILENAME);
    }

    public void refreshJobIds(Collection<Job> jobs, boolean append) {
        try {
            if (!mappingFilePath.toFile().exists()) {
                Files.createFile(mappingFilePath);
            }
            AtomicInteger nextId = append
                ? new AtomicInteger(getLinesAmount(mappingFilePath) + 1)
                : new AtomicInteger(1);
            List<String> lines = jobs.stream()
                .map(job -> String.join(";", String.valueOf(nextId.getAndIncrement()), job.name(), job.url(), JobType.fromName(job.aClass()).name()))
                .toList();
            if (append) {
                Files.write(mappingFilePath, lines, APPEND);
            } else {
                Files.write(mappingFilePath, lines);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error writing mapping file: " + e);
        }
    }

    private int getLinesAmount(Path path) throws IOException {
        try (var lines = Files.lines(path)) {
            return (int) lines.count();
        }
    }

    public Optional<JobDescriptor> findJobById(int id) {
        try {
            List<String> lines = Files.readAllLines(mappingFilePath);
            for (String line : lines) {
                String[] parts = line.split(";");
                if (parts.length == 4 && Integer.parseInt(parts[0]) == id) {
                    return Optional.of(new JobDescriptor(id, parts[1], parts[2], JobType.valueOf(parts[3]), null));
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error reading mapping file: " + e);
        }
        return Optional.empty();
    }

    public Optional<JobDescriptor> findJobByName(String name) {
        try {
            List<String> lines = Files.readAllLines(mappingFilePath);
            for (String line : lines) {
                String[] parts = line.split(";");
                if (parts.length == 4 && parts[1].equals(name)) {
                    return Optional.of(JobDescriptor.builder()
                        .id(Integer.parseInt(parts[0]))
                        .name(parts[1])
                        .url(parts[2])
                        .type(JobType.valueOf(parts[3]))
                        .build());
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error reading mapping file: " + e);
        }
        return Optional.empty();
    }

}
