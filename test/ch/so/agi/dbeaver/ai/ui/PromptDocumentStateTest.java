package ch.so.agi.dbeaver.ai.ui;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PromptDocumentStateTest {
    @Test
    void keepsBindingAndSwitchesToAppendModeAfterSend() {
        PromptDocumentState state = new PromptDocumentState();
        Path promptPath = Path.of("prompts/history.txt");

        state.markSavedDraft(promptPath, "Prompt A");
        state.markSent();

        assertThat(state.boundPath()).isEqualTo(promptPath.toAbsolutePath().normalize());
        assertThat(state.persistenceMode()).isEqualTo(PromptPersistenceMode.APPEND_LOG);
        assertThat(state.savedContent()).isEmpty();
        assertThat(state.lastSavedAppendTail()).isNull();
        assertThat(state.isDirty("")).isFalse();
        assertThat(state.isDirty("Prompt B")).isTrue();
    }

    @Test
    void tracksSavedAppendEntryForCurrentDraft() {
        PromptDocumentState state = new PromptDocumentState();
        Path promptPath = Path.of("prompts/history.txt");

        state.markSavedDraft(promptPath, "Prompt A");
        state.markSent();
        state.markSavedLogEntry(promptPath, "Prompt B", "\n\n===== USER MESSAGE | 2026-03-10 14:32:01 CET =====\nPrompt B");

        assertThat(state.boundPath()).isEqualTo(promptPath.toAbsolutePath().normalize());
        assertThat(state.persistenceMode()).isEqualTo(PromptPersistenceMode.APPEND_LOG);
        assertThat(state.savedContent()).isEqualTo("Prompt B");
        assertThat(state.lastSavedAppendTail()).contains("Prompt B");
        assertThat(state.isDirty("Prompt B")).isFalse();
        assertThat(state.isDirty("Prompt B angepasst")).isTrue();
    }

    @Test
    void openedFileReturnsToOverwriteMode() {
        PromptDocumentState state = new PromptDocumentState();
        Path promptPath = Path.of("prompts/history.txt");

        state.markSavedLogEntry(promptPath, "Prompt B", "\n\n===== USER MESSAGE | 2026-03-10 14:32:01 CET =====\nPrompt B");
        state.markOpened(promptPath, "Bestehender Dateiinhalt");

        assertThat(state.boundPath()).isEqualTo(promptPath.toAbsolutePath().normalize());
        assertThat(state.persistenceMode()).isEqualTo(PromptPersistenceMode.OVERWRITE_DRAFT);
        assertThat(state.lastSavedAppendTail()).isNull();
        assertThat(state.isDirty("Bestehender Dateiinhalt")).isFalse();
        assertThat(state.isDirty("Geaenderter Dateiinhalt")).isTrue();
    }

    @Test
    void resetToUntitledClearsBindingAndAppendTracking() {
        PromptDocumentState state = new PromptDocumentState();

        state.markSavedLogEntry(
            Path.of("prompts/history.txt"),
            "Prompt B",
            "\n\n===== USER MESSAGE | 2026-03-10 14:32:01 CET =====\nPrompt B"
        );
        state.resetToUntitled("");

        assertThat(state.hasBoundPath()).isFalse();
        assertThat(state.boundPath()).isNull();
        assertThat(state.persistenceMode()).isEqualTo(PromptPersistenceMode.OVERWRITE_DRAFT);
        assertThat(state.savedContent()).isEmpty();
        assertThat(state.lastSavedAppendTail()).isNull();
    }
}
