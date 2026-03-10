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

    void saveDraft(Path path, String content) throws IOException {
        Files.writeString(normalize(path), content == null ? "" : content, StandardCharsets.UTF_8);
    }

    String appendLogEntry(Path path, PromptLogEntry entry) throws IOException {
        Path normalizedPath = normalize(path);
        String existing = Files.exists(normalizedPath) ? Files.readString(normalizedPath, StandardCharsets.UTF_8) : "";
        String appendedTail = buildTail(existing, entry.serialize());
        Files.writeString(normalizedPath, existing + appendedTail, StandardCharsets.UTF_8);
        return appendedTail;
    }

    String replaceLastLogEntryOrAppend(Path path, String previousTail, PromptLogEntry entry) throws IOException {
        Path normalizedPath = normalize(path);
        String existing = Files.exists(normalizedPath) ? Files.readString(normalizedPath, StandardCharsets.UTF_8) : "";

        if (previousTail != null && !previousTail.isBlank() && existing.endsWith(previousTail)) {
            String base = existing.substring(0, existing.length() - previousTail.length());
            String replacementTail = buildTail(base, entry.serialize());
            Files.writeString(normalizedPath, base + replacementTail, StandardCharsets.UTF_8);
            return replacementTail;
        }

        return appendLogEntry(normalizedPath, entry);
    }

    private String buildTail(String existingContent, String serializedEntry) {
        return existingContent == null || existingContent.isEmpty() ? serializedEntry : "\n\n" + serializedEntry;
    }

    private Path normalize(Path path) {
        return Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    }
}
