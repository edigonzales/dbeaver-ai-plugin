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
        AiSettings settings = new AiSettingsService().loadSettings(store);

        assertThat(settings.effectiveEndpoints()).hasSize(1);
        assertThat(settings.effectiveEndpoints().get(0).id()).isEqualTo(AiSettings.BUILTIN_OPENAI_ENDPOINT_ID);
        assertThat(settings.systemPrompt()).isEqualTo(AiSettings.DEFAULT_SYSTEM_PROMPT);
        assertThat(settings.timeoutSeconds()).isEqualTo(AiSettings.DEFAULT_TIMEOUT_SECONDS);
    }

    @Test
    void loadSettings_usesProvidedEndpointValuesAndDedupesModels() {
        InMemoryPreferenceStore store = new InMemoryPreferenceStore();
        store.setValue(AiPreferenceConstants.PREF_LLM_ENDPOINT_COUNT, 2);
        store.setValue(AiPreferenceConstants.PREF_LLM_ENDPOINT_ID_PREFIX + "0.id", "ep-1");
        store.setValue(AiPreferenceConstants.PREF_LLM_ENDPOINT_ID_PREFIX + "0.baseUrl", "https://custom.api/v1");
        store.setValue(AiPreferenceConstants.PREF_LLM_ENDPOINT_ID_PREFIX + "0.models", "m1, m2, m1");
        store.setValue(AiPreferenceConstants.PREF_LLM_ENDPOINT_ID_PREFIX + "1.id", "ep-2");
        store.setValue(AiPreferenceConstants.PREF_LLM_ENDPOINT_ID_PREFIX + "1.baseUrl", "https://custom.api/v1");
        store.setValue(AiPreferenceConstants.PREF_LLM_ENDPOINT_ID_PREFIX + "1.models", "x");

        AiSettings settings = new AiSettingsService().loadSettings(store);

        assertThat(settings.endpoints()).hasSize(1);
        assertThat(settings.endpoints().get(0).id()).isEqualTo("ep-1");
        assertThat(settings.endpoints().get(0).baseUrl()).isEqualTo("https://custom.api/v1");
        assertThat(settings.endpoints().get(0).models()).containsExactly("m1", "m2");
    }

    @Test
    void saveSettings_persistsUserEndpointsAndGeneralSettings() {
        InMemoryPreferenceStore store = new InMemoryPreferenceStore();
        AiSettings settings = new AiSettings(
            java.util.List.of(
                LlmEndpointConfig.user("ep-1", "https://one.example/v1", java.util.List.of("a", "b")),
                LlmEndpointConfig.user("ep-2", "https://two.example/v1", java.util.List.of())
            ),
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
            1.5,
            123
        );

        AiSettingsService service = new AiSettingsService();
        service.saveSettings(store, settings);

        assertThat(store.getInt(AiPreferenceConstants.PREF_LLM_ENDPOINT_COUNT)).isEqualTo(2);
        assertThat(store.getString(AiPreferenceConstants.PREF_LLM_ENDPOINT_ID_PREFIX + "0.id")).isEqualTo("ep-1");
        assertThat(store.getString(AiPreferenceConstants.PREF_LLM_ENDPOINT_ID_PREFIX + "0.baseUrl")).isEqualTo("https://one.example/v1");
        assertThat(store.getString(AiPreferenceConstants.PREF_LLM_ENDPOINT_ID_PREFIX + "0.models")).isEqualTo("a,b");
        assertThat(store.getString(AiPreferenceConstants.PREF_LLM_ENDPOINT_ID_PREFIX + "1.models")).isEqualTo("");
        assertThat(store.getInt(AiPreferenceConstants.PREF_TIMEOUT_SECONDS)).isEqualTo(123);
        assertThat(store.saved).isTrue();
    }

    @Test
    void loadSettings_ignoresLegacyBaseUrlAndModelPreferences() {
        InMemoryPreferenceStore store = new InMemoryPreferenceStore();
        store.setValue("ch.so.agi.dbeaver.ai.baseUrl", "https://legacy.example/v1");
        store.setValue("ch.so.agi.dbeaver.ai.model", "legacy-model");

        AiSettings settings = new AiSettingsService().loadSettings(store);

        assertThat(settings.endpoints()).isEmpty();
        assertThat(settings.effectiveEndpoints().get(0).baseUrl()).isEqualTo(AiSettings.BUILTIN_OPENAI_BASE_URL);
    }

    @Test
    void secretKeyForEndpoint_usesBuiltinAndEndpointSpecificKeys() {
        AiSettingsService service = new AiSettingsService();

        assertThat(service.secretKeyForEndpoint(AiSettings.BUILTIN_OPENAI_ENDPOINT_ID))
            .isEqualTo(AiPreferenceConstants.SECRET_OPENAI_API_TOKEN);
        assertThat(service.secretKeyForEndpoint("abc-123"))
            .isEqualTo("ch.so.agi.dbeaver.ai.endpoint.abc-123.apiToken");
    }

    @Test
    void parseModelsCsv_trimsAndDeduplicates() {
        assertThat(AiSettingsService.parseModelsCsv("a, b, ,a"))
            .containsExactly("a", "b");
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
