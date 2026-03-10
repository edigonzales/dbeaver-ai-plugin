package ch.so.agi.dbeaver.ai.ui;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PromptDocumentStateTest {
    @Test
    void tracksDirtyStateAcrossSaveSaveAsOpenAndReset() {
        PromptDocumentState state = new PromptDocumentState();

        state.resetToUntitled("");
        assertThat(state.hasBoundPath()).isFalse();
        assertThat(state.isDirty("")).isFalse();
        assertThat(state.isDirty("Frage 1")).isTrue();

        Path originalPath = Path.of("prompts/original.txt");
        state.markSaved(originalPath, "Frage 1");
        assertThat(state.boundPath()).isEqualTo(originalPath.toAbsolutePath().normalize());
        assertThat(state.isDirty("Frage 1")).isFalse();
        assertThat(state.isDirty("Frage 2")).isTrue();

        Path saveAsPath = Path.of("prompts/neu.txt");
        state.markSaved(saveAsPath, "Frage 2");
        assertThat(state.boundPath()).isEqualTo(saveAsPath.toAbsolutePath().normalize());
        assertThat(state.isDirty("Frage 2")).isFalse();

        state.markOpened(originalPath, "Geladener Prompt");
        assertThat(state.boundPath()).isEqualTo(originalPath.toAbsolutePath().normalize());
        assertThat(state.isDirty("Geladener Prompt")).isFalse();
        assertThat(state.isDirty("Lokal geändert")).isTrue();

        state.resetToUntitled("");
        assertThat(state.hasBoundPath()).isFalse();
        assertThat(state.boundPath()).isNull();
        assertThat(state.savedContent()).isEmpty();
        assertThat(state.isDirty("")).isFalse();
        assertThat(state.isDirty("Neuer Entwurf")).isTrue();
    }

    @Test
    void resetToUntitledClearsExistingFileBinding() {
        PromptDocumentState state = new PromptDocumentState();

        state.markOpened(Path.of("prompts/existing.txt"), "Bestehender Inhalt");
        state.resetToUntitled("");

        assertThat(state.hasBoundPath()).isFalse();
        assertThat(state.boundPath()).isNull();
        assertThat(state.savedContent()).isEmpty();
        assertThat(state.isDirty("")).isFalse();
    }
}
