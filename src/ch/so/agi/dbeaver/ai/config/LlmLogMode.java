package ch.so.agi.dbeaver.ai.config;

public enum LlmLogMode {
    OFF,
    METADATA,
    FULL;

    public static LlmLogMode fromPreferenceValue(String value) {
        if (value == null || value.isBlank()) {
            return METADATA;
        }
        try {
            return LlmLogMode.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return METADATA;
        }
    }
}
