package ch.so.agi.dbeaver.ai.ui;

import java.nio.file.Path;
import java.util.Objects;

final class PromptDocumentState {
    private Path boundPath;
    private String savedContent = "";

    Path boundPath() {
        return boundPath;
    }

    boolean hasBoundPath() {
        return boundPath != null;
    }

    String savedContent() {
        return savedContent;
    }

    boolean isDirty(String currentContent) {
        return !savedContent.equals(normalize(currentContent));
    }

    void markOpened(Path path, String content) {
        this.boundPath = Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
        this.savedContent = normalize(content);
    }

    void markSaved(Path path, String content) {
        markOpened(path, content);
    }

    void resetToUntitled(String content) {
        this.boundPath = null;
        this.savedContent = normalize(content);
    }

    private String normalize(String content) {
        return content == null ? "" : content;
    }
}
