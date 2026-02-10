package ch.so.agi.dbeaver.ai.model;

import java.time.Instant;
import java.util.Objects;

public final class ChatMessage {
    private final ChatRole role;
    private final String text;
    private final Instant timestamp;

    public ChatMessage(ChatRole role, String text, Instant timestamp) {
        this.role = Objects.requireNonNull(role, "role");
        this.text = Objects.requireNonNull(text, "text");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp");
    }

    public ChatRole role() {
        return role;
    }

    public String text() {
        return text;
    }

    public Instant timestamp() {
        return timestamp;
    }
}
