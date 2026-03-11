package ch.so.agi.dbeaver.ai.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

final class MessageLogService {
    private static final String MESSAGE_LOG_FILE_NAME = ".dbeaver-ai-messages.log";

    private final Path messageLogPath;

    MessageLogService() {
        this(Path.of(System.getProperty("user.home"), MESSAGE_LOG_FILE_NAME));
    }

    MessageLogService(Path messageLogPath) {
        this.messageLogPath = normalize(messageLogPath);
    }

    Path messageLogPath() {
        return messageLogPath;
    }

    void appendUserMessage(String promptText) throws IOException {
        appendEntry(PromptLogEntry.now(promptText));
    }

    void appendEntry(PromptLogEntry entry) throws IOException {
        Objects.requireNonNull(entry, "entry");
        Path parent = messageLogPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        String serializedEntry = entry.serialize();
        if (Files.notExists(messageLogPath) || Files.size(messageLogPath) == 0L) {
            Files.writeString(
                messageLogPath,
                serializedEntry,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
            );
            return;
        }

        Files.writeString(
            messageLogPath,
            "\n\n" + serializedEntry,
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
            StandardOpenOption.WRITE
        );
    }

    private Path normalize(Path path) {
        return Objects.requireNonNull(path, "path").toAbsolutePath().normalize();
    }
}
