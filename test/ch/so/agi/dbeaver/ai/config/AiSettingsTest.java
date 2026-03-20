package ch.so.agi.dbeaver.ai.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AiSettingsTest {
    @Test
    void normalizesInvalidInputsToSafeDefaults() {
        AiSettings settings = new AiSettings(
            List.of(LlmEndpointConfig.user(" e1 ", " https://example.com/v1 ", List.of("m1", "m1", " "))),
            "",
            -1,
            -5,
            0,
            true,
            true,
            -1,
            10,
            -5,
            -7,
            null,
            true,
            9.0,
            1
        );

        assertThat(settings.systemPrompt()).isEqualTo(AiSettings.DEFAULT_SYSTEM_PROMPT);
        assertThat(settings.sampleRowLimit()).isEqualTo(1);
        assertThat(settings.maxReferencedTables()).isEqualTo(1);
        assertThat(settings.maxColumnsPerSample()).isEqualTo(1);
        assertThat(settings.historySize()).isEqualTo(0);
        assertThat(settings.maxContextTokens()).isEqualTo(100);
        assertThat(settings.mentionProposalLimit()).isEqualTo(1);
        assertThat(settings.mentionCandidateLimit()).isEqualTo(1);
        assertThat(settings.includeSampleRows()).isFalse();
        assertThat(settings.llmLogMode()).isEqualTo(LlmLogMode.METADATA);
        assertThat(settings.langchainHttpLogging()).isTrue();
        assertThat(settings.temperature()).isEqualTo(2.0);
        assertThat(settings.timeoutSeconds()).isEqualTo(AiSettings.MIN_TIMEOUT_SECONDS);
        assertThat(settings.timeout()).isEqualTo(Duration.ofSeconds(AiSettings.MIN_TIMEOUT_SECONDS));
        assertThat(settings.endpoints()).hasSize(1);
        assertThat(settings.endpoints().get(0).baseUrl()).isEqualTo("https://example.com/v1");
        assertThat(settings.endpoints().get(0).models()).containsExactly("m1");
    }

    @Test
    void effectiveEndpointsAlwaysContainBuiltinOpenAiConfig() {
        AiSettings settings = new AiSettings(
            List.of(LlmEndpointConfig.user("e1", "https://custom.example/v1", List.of("custom-model"))),
            "prompt",
            42,
            8,
            99,
            true,
            true,
            12,
            4_000,
            40,
            100_000,
            LlmLogMode.METADATA,
            false,
            0.0,
            120
        );

        assertThat(settings.effectiveEndpoints()).hasSize(2);
        assertThat(settings.effectiveEndpoints().get(0).id()).isEqualTo(AiSettings.BUILTIN_OPENAI_ENDPOINT_ID);
        assertThat(settings.effectiveEndpoints().get(0).baseUrl()).isEqualTo(AiSettings.BUILTIN_OPENAI_BASE_URL);
        assertThat(settings.effectiveEndpoints().get(0).models()).containsExactly("gpt-5", "gpt-5-mini", "gpt-5-nano");
        assertThat(settings.toChatRequestOptions().includeSampleRows()).isFalse();
        assertThat(settings.toChatRequestOptions().sampleRowLimit()).isEqualTo(AiSettings.DEFAULT_SAMPLE_ROW_LIMIT);
        assertThat(settings.toChatRequestOptions().maxColumnsPerSample()).isEqualTo(AiSettings.DEFAULT_MAX_COLUMNS_PER_SAMPLE);
        assertThat(settings.timeout()).isEqualTo(Duration.ofSeconds(120));
    }

    @Test
    void resolveSelectionFallsBackToFirstSendableCombination() {
        AiSettings settings = new AiSettings(
            List.of(
                LlmEndpointConfig.user("empty", "https://empty.example/v1", List.of()),
                LlmEndpointConfig.user("custom", "https://custom.example/v1", List.of("m1", "m2"))
            ),
            "prompt",
            1,
            1,
            1,
            true,
            false,
            1,
            100,
            1,
            1,
            LlmLogMode.OFF,
            false,
            0.0,
            100
        );

        AiSettings.EndpointSelection selection = settings.resolveSelection("unknown", "unknown");

        assertThat(selection.endpoint().id()).isEqualTo(AiSettings.BUILTIN_OPENAI_ENDPOINT_ID);
        assertThat(selection.modelName()).isEqualTo("gpt-5");
    }

    @Test
    void clampsTimeoutToConfiguredBounds() {
        AiSettings tooSmall = new AiSettings(
            List.of(),
            "prompt",
            1,
            1,
            1,
            true,
            false,
            1,
            100,
            1,
            1,
            LlmLogMode.OFF,
            false,
            0.0,
            5
        );
        AiSettings tooLarge = new AiSettings(
            List.of(),
            "prompt",
            1,
            1,
            1,
            true,
            false,
            1,
            100,
            1,
            1,
            LlmLogMode.OFF,
            false,
            0.0,
            999
        );

        assertThat(tooSmall.timeoutSeconds()).isEqualTo(AiSettings.MIN_TIMEOUT_SECONDS);
        assertThat(tooLarge.timeoutSeconds()).isEqualTo(AiSettings.MAX_TIMEOUT_SECONDS);
    }
}
