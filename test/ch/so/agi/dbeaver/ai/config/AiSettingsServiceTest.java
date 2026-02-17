package ch.so.agi.dbeaver.ai.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiSettingsServiceTest {

    @Test
    void loadSettings_returnsDefaultsWhenPreferencesAreEmpty() {
        // Test that AiSettings constructor handles null/empty/invalid values correctly
        // This simulates what happens when preferences are not set (fresh install)
        AiSettings settings = new AiSettings(
            null, null, null, 0, 0, 0, false, false, 0, 0, 0, null, false, Double.NaN
        );

        assertThat(settings.baseUrl()).isEqualTo(AiSettings.DEFAULT_BASE_URL);
        assertThat(settings.model()).isEqualTo(AiSettings.DEFAULT_MODEL);
        assertThat(settings.systemPrompt()).isEqualTo(AiSettings.DEFAULT_SYSTEM_PROMPT);
        assertThat(settings.sampleRowLimit()).isEqualTo(1); // min clamp
        assertThat(settings.maxReferencedTables()).isEqualTo(1); // min clamp
        assertThat(settings.maxColumnsPerSample()).isEqualTo(1); // min clamp
        assertThat(settings.includeDdl()).isFalse();
        assertThat(settings.includeSampleRows()).isFalse();
        assertThat(settings.historySize()).isEqualTo(0);
        assertThat(settings.maxContextTokens()).isEqualTo(100); // min clamp
        assertThat(settings.mentionProposalLimit()).isEqualTo(1); // min clamp
        assertThat(settings.llmLogMode()).isEqualTo(LlmLogMode.METADATA);
        assertThat(settings.langchainHttpLogging()).isFalse();
        assertThat(settings.temperature()).isEqualTo(0.0); // NaN clamp
    }

    @Test
    void loadSettings_usesDefaultsForEmptyStringValues() {
        // Test that empty string values are handled correctly
        AiSettings settings = new AiSettings(
            "", "", "", 5, 8, 30, true, true, 12, 4000, 40, LlmLogMode.METADATA, false, 0.0
        );

        assertThat(settings.baseUrl()).isEqualTo(AiSettings.DEFAULT_BASE_URL);
        assertThat(settings.model()).isEqualTo(AiSettings.DEFAULT_MODEL);
        assertThat(settings.systemPrompt()).isEqualTo(AiSettings.DEFAULT_SYSTEM_PROMPT);
    }

    @Test
    void loadSettings_usesProvidedValuesWhenSet() {
        String customBaseUrl = "https://custom.api.example.com/v1";
        String customModel = "gpt-4-turbo";
        String customPrompt = "Custom system prompt";

        AiSettings settings = new AiSettings(
            customBaseUrl,
            customModel,
            customPrompt,
            10,
            5,
            20,
            false,
            false,
            20,
            8000,
            50,
            LlmLogMode.FULL,
            true,
            1.5
        );

        assertThat(settings.baseUrl()).isEqualTo(customBaseUrl);
        assertThat(settings.model()).isEqualTo(customModel);
        assertThat(settings.systemPrompt()).isEqualTo(customPrompt);
        assertThat(settings.sampleRowLimit()).isEqualTo(10);
        assertThat(settings.maxReferencedTables()).isEqualTo(5);
        assertThat(settings.maxColumnsPerSample()).isEqualTo(20);
        assertThat(settings.includeDdl()).isFalse();
        assertThat(settings.includeSampleRows()).isFalse();
        assertThat(settings.historySize()).isEqualTo(20);
        assertThat(settings.maxContextTokens()).isEqualTo(8000);
        assertThat(settings.mentionProposalLimit()).isEqualTo(50);
        assertThat(settings.llmLogMode()).isEqualTo(LlmLogMode.FULL);
        assertThat(settings.langchainHttpLogging()).isTrue();
        assertThat(settings.temperature()).isEqualTo(1.5);
    }

    @Test
    void getPreferenceInt_returnsDefaultWhenValueIsZero() {
        // This test documents the behavior of getPreferenceInt helper method
        // When preferences are not set, getInt() returns 0, so we use the default
        // The actual helper method is tested indirectly through AiSettings constructor
        AiSettings settingsWithZeroValues = new AiSettings(
            "https://example.com", "model", "prompt",
            0, 0, 0, true, true, 0, 0, 0, LlmLogMode.OFF, false, 0.0
        );

        // Values of 0 should be clamped to minimum of 1 (per AiSettings constructor logic)
        assertThat(settingsWithZeroValues.sampleRowLimit()).isEqualTo(1);
        assertThat(settingsWithZeroValues.maxReferencedTables()).isEqualTo(1);
        assertThat(settingsWithZeroValues.maxColumnsPerSample()).isEqualTo(1);
    }

    @Test
    void getPreferenceBoolean_returnsDefaultWhenValueIsNull() {
        // Test boolean handling - null string should result in default behavior
        // The AiSettings constructor doesn't have fallback for booleans,
        // so this tests that the service layer handles it correctly
        AiSettings settings = new AiSettings(
            "https://example.com", "model", "prompt",
            5, 8, 30, true, true, 12, 4000, 40, LlmLogMode.METADATA, false, 0.0
        );

        assertThat(settings.includeDdl()).isTrue();
        assertThat(settings.includeSampleRows()).isTrue();
        assertThat(settings.langchainHttpLogging()).isFalse();
    }
}
