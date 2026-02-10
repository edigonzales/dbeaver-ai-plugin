package ch.so.agi.dbeaver.ai.mention;

import java.util.Objects;

public record MentionToken(int startOffset, int endOffsetExclusive, String raw) {
    public MentionToken {
        if (startOffset < 0 || endOffsetExclusive < startOffset) {
            throw new IllegalArgumentException("Invalid mention token offsets");
        }
        Objects.requireNonNull(raw, "raw");
    }
}
