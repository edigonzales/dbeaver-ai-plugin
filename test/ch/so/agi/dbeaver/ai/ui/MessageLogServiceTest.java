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

class MessageLogServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void appendsMessagesAsTimestampedBlocks() throws IOException {
        Path messageLogPath = tempDir.resolve(".dbeaver-ai-messages.log");
        MessageLogService service = new MessageLogService(messageLogPath);

        service.appendEntry(entry("Prompt A", 14, 32, 1));
        service.appendEntry(entry("Prompt B", 14, 33, 2));

        assertThat(Files.readString(messageLogPath)).isEqualTo(
            "===== USER MESSAGE | 2026-03-10 14:32:01 CET =====\nPrompt A"
                + "\n\n===== USER MESSAGE | 2026-03-10 14:33:02 CET =====\nPrompt B"
        );
    }

    @Test
    void repeatedMessagesAreLoggedAsSeparateEntries() throws IOException {
        Path messageLogPath = tempDir.resolve(".dbeaver-ai-messages.log");
        MessageLogService service = new MessageLogService(messageLogPath);

        service.appendEntry(entry("Gleicher Prompt", 14, 32, 1));
        service.appendEntry(entry("Gleicher Prompt", 14, 32, 2));

        assertThat(Files.readString(messageLogPath)).contains(
            "===== USER MESSAGE | 2026-03-10 14:32:01 CET =====\nGleicher Prompt"
        ).contains(
            "===== USER MESSAGE | 2026-03-10 14:32:02 CET =====\nGleicher Prompt"
        );
    }

    @Test
    void createsParentDirectoriesWhenNeeded() throws IOException {
        Path messageLogPath = tempDir.resolve("nested/logs/.dbeaver-ai-messages.log");
        MessageLogService service = new MessageLogService(messageLogPath);

        service.appendEntry(entry("Prompt A", 14, 32, 1));

        assertThat(Files.readString(messageLogPath)).isEqualTo(
            "===== USER MESSAGE | 2026-03-10 14:32:01 CET =====\nPrompt A"
        );
    }

    @Test
    void appendFailsForDirectoryTarget() throws IOException {
        Path directoryTarget = Files.createDirectory(tempDir.resolve("message-log-dir"));
        MessageLogService service = new MessageLogService(directoryTarget);

        assertThatIOException().isThrownBy(() -> service.appendUserMessage("Prompt"));
    }

    private PromptLogEntry entry(String promptText, int hour, int minute, int second) {
        return new PromptLogEntry(
            promptText,
            ZonedDateTime.of(2026, 3, 10, hour, minute, second, 0, ZoneId.of("Europe/Zurich"))
        );
    }
}
