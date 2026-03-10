package ch.so.agi.dbeaver.ai.ui;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.time.ZonedDateTime;

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
    void appendLogEntryAddsTimestampedBlockAfterExistingDraft() throws IOException {
        Path promptFile = tempDir.resolve("prompt.txt");
        service.saveDraft(promptFile, "Prompt A");

        String tail = service.appendLogEntry(promptFile, entry("Prompt B", 14, 32, 1));

        assertThat(tail).isEqualTo("\n\n===== USER MESSAGE | 2026-03-10 14:32:01 CET =====\nPrompt B");
        assertThat(Files.readString(promptFile)).isEqualTo(
            "Prompt A\n\n===== USER MESSAGE | 2026-03-10 14:32:01 CET =====\nPrompt B"
        );
    }

    @Test
    void replaceLastLogEntryOrAppendReplacesMatchingTail() throws IOException {
        Path promptFile = tempDir.resolve("prompt.txt");
        service.saveDraft(promptFile, "Prompt A");
        String originalTail = service.appendLogEntry(promptFile, entry("Prompt B", 14, 32, 1));

        String replacementTail = service.replaceLastLogEntryOrAppend(promptFile, originalTail, entry("Prompt B2", 14, 33, 1));

        assertThat(replacementTail).isEqualTo("\n\n===== USER MESSAGE | 2026-03-10 14:33:01 CET =====\nPrompt B2");
        assertThat(Files.readString(promptFile)).isEqualTo(
            "Prompt A\n\n===== USER MESSAGE | 2026-03-10 14:33:01 CET =====\nPrompt B2"
        );
    }

    @Test
    void replaceLastLogEntryOrAppendFallsBackToAppendWhenTailNoLongerMatches() throws IOException {
        Path promptFile = tempDir.resolve("prompt.txt");
        service.saveDraft(promptFile, "Prompt A");
        String originalTail = service.appendLogEntry(promptFile, entry("Prompt B", 14, 32, 1));
        Files.writeString(promptFile, Files.readString(promptFile) + "\n\nEXTERN");

        String replacementTail = service.replaceLastLogEntryOrAppend(promptFile, originalTail, entry("Prompt C", 14, 34, 1));

        assertThat(replacementTail).isEqualTo("\n\n===== USER MESSAGE | 2026-03-10 14:34:01 CET =====\nPrompt C");
        assertThat(Files.readString(promptFile)).isEqualTo(
            "Prompt A\n\n===== USER MESSAGE | 2026-03-10 14:32:01 CET =====\nPrompt B\n\nEXTERN"
                + "\n\n===== USER MESSAGE | 2026-03-10 14:34:01 CET =====\nPrompt C"
        );
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

    private PromptLogEntry entry(String promptText, int hour, int minute, int second) {
        return new PromptLogEntry(
            promptText,
            ZonedDateTime.of(2026, 3, 10, hour, minute, second, 0, ZoneId.of("Europe/Zurich"))
        );
    }
}
