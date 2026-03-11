package ch.so.agi.dbeaver.ai.config;

import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AiSettingsServiceTest {

    @Test
    void loadSettings_returnsDefaultsWhenPreferencesAreEmpty() {
        InMemoryPreferenceStore store = new InMemoryPreferenceStore();
        AiSettingsService service = new AiSettingsService();
        AiSettings settings = service.loadSettings(store);

        assertThat(settings.baseUrl()).isEqualTo(AiSettings.DEFAULT_BASE_URL);
        assertThat(settings.model()).isEqualTo(AiSettings.DEFAULT_MODEL);
        assertThat(settings.systemPrompt()).isEqualTo(AiSettings.DEFAULT_SYSTEM_PROMPT);
        assertThat(settings.sampleRowLimit()).isEqualTo(AiSettings.DEFAULT_SAMPLE_ROW_LIMIT);
        assertThat(settings.maxReferencedTables()).isEqualTo(AiSettings.DEFAULT_MAX_REFERENCED_TABLES);
        assertThat(settings.maxColumnsPerSample()).isEqualTo(AiSettings.DEFAULT_MAX_COLUMNS_PER_SAMPLE);
        assertThat(settings.includeDdl()).isEqualTo(AiSettings.DEFAULT_INCLUDE_DDL);
        assertThat(settings.includeSampleRows()).isFalse();
        assertThat(settings.historySize()).isEqualTo(AiSettings.DEFAULT_HISTORY_SIZE);
        assertThat(settings.maxContextTokens()).isEqualTo(AiSettings.DEFAULT_MAX_CONTEXT_TOKENS);
        assertThat(settings.mentionProposalLimit()).isEqualTo(AiSettings.DEFAULT_MENTION_PROPOSAL_LIMIT);
        assertThat(settings.mentionCandidateLimit()).isEqualTo(AiSettings.DEFAULT_MENTION_CANDIDATE_LIMIT);
        assertThat(settings.llmLogMode()).isEqualTo(AiSettings.DEFAULT_LLM_LOG_MODE);
        assertThat(settings.langchainHttpLogging()).isEqualTo(AiSettings.DEFAULT_LANGCHAIN_HTTP_LOGGING);
        assertThat(settings.temperature()).isEqualTo(AiSettings.DEFAULT_TEMPERATURE);
    }

    @Test
    void loadSettings_ignoresLegacyStoredIncludeSampleRowsFlag() {
        InMemoryPreferenceStore store = new InMemoryPreferenceStore();
        store.setValue(AiPreferenceConstants.PREF_INCLUDE_SAMPLE_ROWS, true);
        store.setValue(AiPreferenceConstants.PREF_INCLUDE_DDL, true);
        store.setValue(AiPreferenceConstants.PREF_SAMPLE_ROW_LIMIT, 11);

        AiSettings settings = new AiSettingsService().loadSettings(store);

        assertThat(settings.includeSampleRows()).isFalse();
        assertThat(settings.includeDdl()).isTrue();
        assertThat(settings.sampleRowLimit()).isEqualTo(11);
    }

    @Test
    void saveSettings_persistsFalseForDisabledSampleRowsFlag() {
        InMemoryPreferenceStore store = new InMemoryPreferenceStore();
        AiSettings settings = new AiSettings(
            "https://custom.api.example.com/v1",
            "gpt-4-turbo",
            "Custom system prompt",
            10,
            5,
            20,
            false,
            true,
            20,
            8000,
            50,
            60,
            LlmLogMode.FULL,
            true,
            1.5
        );
        AiSettingsService service = new AiSettingsService();

        service.saveSettings(store, settings);

        assertThat(store.getBoolean(AiPreferenceConstants.PREF_INCLUDE_SAMPLE_ROWS)).isFalse();
        assertThat(store.saved).isTrue();
    }

    @Test
    void loadSettings_usesProvidedValuesWhenSet() {
        InMemoryPreferenceStore store = new InMemoryPreferenceStore();
        String customBaseUrl = "https://custom.api.example.com/v1";
        String customModel = "gpt-4-turbo";
        String customPrompt = "Custom system prompt";
        store.setValue(AiPreferenceConstants.PREF_BASE_URL, customBaseUrl);
        store.setValue(AiPreferenceConstants.PREF_MODEL, customModel);
        store.setValue(AiPreferenceConstants.PREF_SYSTEM_PROMPT, customPrompt);
        store.setValue(AiPreferenceConstants.PREF_SAMPLE_ROW_LIMIT, 10);
        store.setValue(AiPreferenceConstants.PREF_MAX_REFERENCED_TABLES, 5);
        store.setValue(AiPreferenceConstants.PREF_MAX_COLUMNS_PER_SAMPLE, 20);
        store.setValue(AiPreferenceConstants.PREF_INCLUDE_DDL, false);
        store.setValue(AiPreferenceConstants.PREF_INCLUDE_SAMPLE_ROWS, true);
        store.setValue(AiPreferenceConstants.PREF_HISTORY_SIZE, 20);
        store.setValue(AiPreferenceConstants.PREF_MAX_CONTEXT_TOKENS, 8000);
        store.setValue(AiPreferenceConstants.PREF_MENTION_PROPOSAL_LIMIT, 50);
        store.setValue(AiPreferenceConstants.PREF_MENTION_CANDIDATE_LIMIT, 60);
        store.setValue(AiPreferenceConstants.PREF_LLM_LOG_MODE, LlmLogMode.FULL.name());
        store.setValue(AiPreferenceConstants.PREF_LANGCHAIN_HTTP_LOGGING, true);
        store.setValue(AiPreferenceConstants.PREF_TEMPERATURE, "1.5");

        AiSettings settings = new AiSettingsService().loadSettings(store);

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
        assertThat(settings.mentionCandidateLimit()).isEqualTo(60);
        assertThat(settings.llmLogMode()).isEqualTo(LlmLogMode.FULL);
        assertThat(settings.langchainHttpLogging()).isTrue();
        assertThat(settings.temperature()).isEqualTo(1.5);
    }

    private static final class InMemoryPreferenceStore implements DBPPreferenceStore {
        private final Map<String, Object> values = new HashMap<>();
        private final Map<String, Object> defaults = new HashMap<>();
        private boolean saved;

        @Override
        public boolean contains(String name) {
            return values.containsKey(name) || defaults.containsKey(name);
        }

        @Override
        public boolean getBoolean(String name) {
            Object value = values.get(name);
            return value instanceof Boolean bool ? bool : Boolean.parseBoolean(getString(name));
        }

        @Override
        public double getDouble(String name) {
            Object value = values.get(name);
            return value instanceof Number number ? number.doubleValue() : 0.0;
        }

        @Override
        public float getFloat(String name) {
            Object value = values.get(name);
            return value instanceof Number number ? number.floatValue() : 0f;
        }

        @Override
        public int getInt(String name) {
            Object value = values.get(name);
            return value instanceof Number number ? number.intValue() : 0;
        }

        @Override
        public long getLong(String name) {
            Object value = values.get(name);
            return value instanceof Number number ? number.longValue() : 0L;
        }

        @Override
        public String getString(String name) {
            Object value = values.get(name);
            return value == null ? null : value.toString();
        }

        @Override
        public boolean getDefaultBoolean(String name) {
            Object value = defaults.get(name);
            return value instanceof Boolean bool && bool;
        }

        @Override
        public double getDefaultDouble(String name) {
            Object value = defaults.get(name);
            return value instanceof Number number ? number.doubleValue() : 0.0;
        }

        @Override
        public float getDefaultFloat(String name) {
            Object value = defaults.get(name);
            return value instanceof Number number ? number.floatValue() : 0f;
        }

        @Override
        public int getDefaultInt(String name) {
            Object value = defaults.get(name);
            return value instanceof Number number ? number.intValue() : 0;
        }

        @Override
        public long getDefaultLong(String name) {
            Object value = defaults.get(name);
            return value instanceof Number number ? number.longValue() : 0L;
        }

        @Override
        public String getDefaultString(String name) {
            Object value = defaults.get(name);
            return value == null ? null : value.toString();
        }

        @Override
        public boolean isDefault(String name) {
            return !values.containsKey(name) && defaults.containsKey(name);
        }

        @Override
        public boolean needsSaving() {
            return saved;
        }

        @Override
        public void setDefault(String name, double value) {
            defaults.put(name, value);
        }

        @Override
        public void setDefault(String name, float value) {
            defaults.put(name, value);
        }

        @Override
        public void setDefault(String name, int value) {
            defaults.put(name, value);
        }

        @Override
        public void setDefault(String name, long value) {
            defaults.put(name, value);
        }

        @Override
        public void setDefault(String name, String defaultObject) {
            defaults.put(name, defaultObject);
        }

        @Override
        public void setDefault(String name, boolean value) {
            defaults.put(name, value);
        }

        @Override
        public void setToDefault(String name) {
            values.remove(name);
        }

        @Override
        public void setValue(String name, double value) {
            values.put(name, value);
        }

        @Override
        public void setValue(String name, float value) {
            values.put(name, value);
        }

        @Override
        public void setValue(String name, int value) {
            values.put(name, value);
        }

        @Override
        public void setValue(String name, long value) {
            values.put(name, value);
        }

        @Override
        public void setValue(String name, String value) {
            values.put(name, value);
        }

        @Override
        public void setValue(String name, boolean value) {
            values.put(name, value);
        }

        @Override
        public void addPropertyChangeListener(DBPPreferenceListener listener) {
        }

        @Override
        public void removePropertyChangeListener(DBPPreferenceListener listener) {
        }

        @Override
        public void firePropertyChangeEvent(String name, Object oldValue, Object newValue) {
        }

        @Override
        public void save() throws IOException {
            saved = true;
        }
    }
}
