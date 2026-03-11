package ch.so.agi.dbeaver.ai.ui;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class PromptDocumentStateTest {
    @Test
    void savedDraftTracksBindingAndDirtyState() {
        PromptDocumentState state = new PromptDocumentState();
        Path promptPath = Path.of("prompts/draft.txt");

        state.markSavedDraft(promptPath, "Prompt A");

        assertThat(state.boundPath()).isEqualTo(promptPath.toAbsolutePath().normalize());
        assertThat(state.hasBoundPath()).isTrue();
        assertThat(state.savedContent()).isEqualTo("Prompt A");
        assertThat(state.isDirty("Prompt A")).isFalse();
        assertThat(state.isDirty("Prompt B")).isTrue();
    }

    @Test
    void openedFileBehavesLikeSavedDraft() {
        PromptDocumentState state = new PromptDocumentState();
        Path promptPath = Path.of("prompts/existing.txt");

        state.markOpened(promptPath, "Bestehender Dateiinhalt");

        assertThat(state.boundPath()).isEqualTo(promptPath.toAbsolutePath().normalize());
        assertThat(state.savedContent()).isEqualTo("Bestehender Dateiinhalt");
        assertThat(state.isDirty("Bestehender Dateiinhalt")).isFalse();
        assertThat(state.isDirty("Geaenderter Dateiinhalt")).isTrue();
    }

    @Test
    void resetToUntitledClearsBindingAndUsesProvidedContentAsBaseline() {
        PromptDocumentState state = new PromptDocumentState();
        state.markSavedDraft(Path.of("prompts/draft.txt"), "Prompt A");

        state.resetToUntitled("");

        assertThat(state.hasBoundPath()).isFalse();
        assertThat(state.boundPath()).isNull();
        assertThat(state.savedContent()).isEmpty();
        assertThat(state.isDirty("")).isFalse();
        assertThat(state.isDirty("Neuer Prompt")).isTrue();
    }
}
