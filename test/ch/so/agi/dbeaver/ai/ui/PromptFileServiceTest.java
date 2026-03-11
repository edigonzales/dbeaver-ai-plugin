package ch.so.agi.dbeaver.ai.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIOException;

class PromptFileServiceTest {
    @TempDir
    Path tempDir;

    private final PromptFileService service = new PromptFileService();

    @Test
    void savesAndLoadsUtf8Prompt() throws IOException {
        Path promptFile = tempDir.resolve("prompt.txt");
        String prompt = "Bitte erklaere Tabelle aeoeue\nSELECT * FROM test;";

        service.saveDraft(promptFile, prompt);

        assertThat(service.load(promptFile)).isEqualTo(prompt);
    }

    @Test
    void saveDraftOverwritesExistingFile() throws IOException {
        Path promptFile = tempDir.resolve("prompt.txt");
        service.saveDraft(promptFile, "Version 1");

        service.saveDraft(promptFile, "Version 2");

        assertThat(Files.readString(promptFile)).isEqualTo("Version 2");
    }

    @Test
    void loadFailsForMissingFile() {
        Path missingFile = tempDir.resolve("missing.txt");

        assertThatIOException().isThrownBy(() -> service.load(missingFile));
    }

    @Test
    void saveDraftFailsForDirectoryTarget() throws IOException {
        Path directoryTarget = Files.createDirectory(tempDir.resolve("prompt-dir"));

        assertThatIOException().isThrownBy(() -> service.saveDraft(directoryTarget, "Inhalt"));
    }
}
