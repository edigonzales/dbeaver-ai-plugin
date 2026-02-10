package ch.so.agi.dbeaver.ai.llm;

import ch.so.agi.dbeaver.ai.model.ChatMessage;
import ch.so.agi.dbeaver.ai.model.ChatRole;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LlmPayloadLoggerTest {
    private final LlmPayloadLogger logger = new LlmPayloadLogger();

    @Test
    void formatsRequestSectionsAndMasksSensitiveValues() {
        LlmRequest request = new LlmRequest(
            "authorization=secret-system",
            "Bitte hilf. token=secret-user",
            "apiKey: secret-context",
            List.of(
                new ChatMessage(ChatRole.USER, "api-key=secret-history", Instant.now()),
                new ChatMessage(ChatRole.ASSISTANT, "ok", Instant.now())
            )
        );

        List<String> parts = logger.formatRequestParts(request);
        String merged = String.join("", parts);

        assertThat(merged).contains("SYSTEM");
        assertThat(merged).contains("HISTORY");
        assertThat(merged).contains("USER");
        assertThat(merged).contains("CONTEXT");
        assertThat(merged).doesNotContain("secret-system");
        assertThat(merged).doesNotContain("secret-user");
        assertThat(merged).doesNotContain("secret-context");
        assertThat(merged).doesNotContain("secret-history");
        assertThat(merged).contains("***");
    }

    @Test
    void splitsLargeResponseIntoBoundedParts() {
        String longResponse = "x".repeat(LlmPayloadLogger.PART_CHAR_LIMIT + 100);

        List<String> parts = logger.formatResponseParts(longResponse);

        assertThat(parts).hasSize(2);
        assertThat(parts.get(0).length()).isLessThanOrEqualTo(LlmPayloadLogger.PART_CHAR_LIMIT);
        assertThat(parts.get(1).length()).isLessThanOrEqualTo(LlmPayloadLogger.PART_CHAR_LIMIT);
    }
}
