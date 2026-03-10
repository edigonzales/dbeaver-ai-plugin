package ch.so.agi.dbeaver.ai.ui;

import java.nio.file.Path;
import java.util.Objects;

final class PromptDocumentState {
    private Path boundPath;
    private String savedContent = "";
    private PromptPersistenceMode persistenceMode = PromptPersistenceMode.OVERWRITE_DRAFT;
    private String lastSavedAppendTail;

    Path boundPath() {
        return boundPath;
    }

    boolean hasBoundPath() {
        return boundPath != null;
    }

    String savedContent() {
        return savedContent;
    }

    PromptPersistenceMode persistenceMode() {
        return persistenceMode;
    }

    String lastSavedAppendTail() {
        return lastSavedAppendTail;
    }

    boolean isDirty(String currentContent) {
        return !savedContent.equals(normalize(currentContent));
    }

    void markOpened(Path path, String content) {
        this.boundPath = normalize(path);
        this.savedContent = normalize(content);
        this.persistenceMode = PromptPersistenceMode.OVERWRITE_DRAFT;
        this.lastSavedAppendTail = null;
    }

    void markSavedDraft(Path path, String content) {
        markOpened(path, content);
    }

    void markSavedLogEntry(Path path, String draftContent, String appendTail) {
        this.boundPath = normalize(path);
        this.savedContent = normalize(draftContent);
        this.persistenceMode = PromptPersistenceMode.APPEND_LOG;
        this.lastSavedAppendTail = appendTail == null || appendTail.isBlank() ? null : appendTail;
    }

    void markSent() {
        if (boundPath == null) {
            resetToUntitled("");
            return;
        }
        this.savedContent = "";
        this.persistenceMode = PromptPersistenceMode.APPEND_LOG;
        this.lastSavedAppendTail = null;
    }

    void resetToUntitled(String content) {
        this.boundPath = null;
        this.savedContent = normalize(content);
        this.persistenceMode = PromptPersistenceMode.OVERWRITE_DRAFT;
        this.lastSavedAppendTail = null;
    }

    private Path normalize(Path path) {
        return Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    }

    private String normalize(String content) {
        return content == null ? "" : content;
    }
}
