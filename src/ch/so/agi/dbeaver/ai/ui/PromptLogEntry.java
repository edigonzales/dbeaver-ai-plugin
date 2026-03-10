package ch.so.agi.dbeaver.ai.ui;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Objects;

final class PromptLogEntry {
    private static final DateTimeFormatter TIMESTAMP_FORMAT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss z", Locale.ENGLISH);

    private final String promptText;
    private final ZonedDateTime timestamp;

    PromptLogEntry(String promptText, ZonedDateTime timestamp) {
        this.promptText = promptText == null ? "" : promptText;
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    static PromptLogEntry now(String promptText) {
        return new PromptLogEntry(promptText, ZonedDateTime.now(ZoneId.systemDefault()));
    }

    String serialize() {
        return "===== USER MESSAGE | " + TIMESTAMP_FORMAT.format(timestamp) + " =====\n" + promptText;
    }
}
