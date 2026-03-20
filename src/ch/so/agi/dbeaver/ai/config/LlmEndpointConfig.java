package ch.so.agi.dbeaver.ai.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

public final class LlmEndpointConfig {
    private final String id;
    private final String baseUrl;
    private final List<String> models;
    private final boolean builtin;

    private LlmEndpointConfig(String id, String baseUrl, List<String> models, boolean builtin) {
        this.id = normalizeRequired(id, "id");
        this.baseUrl = normalizeRequired(baseUrl, "baseUrl");
        this.models = List.copyOf(normalizeModels(models));
        this.builtin = builtin;
    }

    public static LlmEndpointConfig builtin(String id, String baseUrl, List<String> models) {
        return new LlmEndpointConfig(id, baseUrl, models, true);
    }

    public static LlmEndpointConfig user(String id, String baseUrl, List<String> models) {
        return new LlmEndpointConfig(id, baseUrl, models, false);
    }

    private static String normalizeRequired(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return trimmed;
    }

    private static List<String> normalizeModels(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        LinkedHashSet<String> deduped = new LinkedHashSet<>();
        for (String value : values) {
            if (value == null) {
                continue;
            }
            String trimmed = value.trim();
            if (!trimmed.isEmpty()) {
                deduped.add(trimmed);
            }
        }
        return new ArrayList<>(deduped);
    }

    public String id() {
        return id;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public List<String> models() {
        return models;
    }

    public boolean builtin() {
        return builtin;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, baseUrl, models, builtin);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof LlmEndpointConfig other)) {
            return false;
        }
        return builtin == other.builtin
            && id.equals(other.id)
            && baseUrl.equals(other.baseUrl)
            && models.equals(other.models);
    }
}
