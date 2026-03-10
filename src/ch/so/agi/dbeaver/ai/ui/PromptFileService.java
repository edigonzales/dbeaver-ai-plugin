package ch.so.agi.dbeaver.ai.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

final class PromptFileService {
    String load(Path path) throws IOException {
        return Files.readString(normalize(path), StandardCharsets.UTF_8);
    }

    void save(Path path, String content) throws IOException {
        Files.writeString(normalize(path), content == null ? "" : content, StandardCharsets.UTF_8);
    }

    private Path normalize(Path path) {
        return Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    }
}
