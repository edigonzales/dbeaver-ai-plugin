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
        this.boundPath = normalize(path);
        this.savedContent = normalize(content);
    }

    void markSavedDraft(Path path, String content) {
        markOpened(path, content);
    }

    void resetToUntitled(String content) {
        this.boundPath = null;
        this.savedContent = normalize(content);
    }

    private Path normalize(Path path) {
        return Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    }

    private String normalize(String content) {
        return content == null ? "" : content;
    }
}
