package ch.so.agi.dbeaver.ai.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AiSettingsTest {
    @Test
    void normalizesInvalidInputsToSafeDefaults() {
        AiSettings settings = new AiSettings(
            " ",
            "",
            "",
            -1,
            -5,
            0,
            true,
            true,
            -1,
            10,
            -5,
            null,
            true,
            9.0
        );

        assertThat(settings.baseUrl()).isEqualTo(AiSettings.DEFAULT_BASE_URL);
        assertThat(settings.model()).isEqualTo(AiSettings.DEFAULT_MODEL);
        assertThat(settings.systemPrompt()).isEqualTo(AiSettings.DEFAULT_SYSTEM_PROMPT);
        assertThat(settings.sampleRowLimit()).isEqualTo(1);
        assertThat(settings.maxReferencedTables()).isEqualTo(1);
        assertThat(settings.maxColumnsPerSample()).isEqualTo(1);
        assertThat(settings.historySize()).isEqualTo(0);
        assertThat(settings.maxContextTokens()).isEqualTo(100);
        assertThat(settings.mentionProposalLimit()).isEqualTo(1);
        assertThat(settings.llmLogMode()).isEqualTo(LlmLogMode.METADATA);
        assertThat(settings.langchainHttpLogging()).isTrue();
        assertThat(settings.temperature()).isEqualTo(2.0);
    }
}
