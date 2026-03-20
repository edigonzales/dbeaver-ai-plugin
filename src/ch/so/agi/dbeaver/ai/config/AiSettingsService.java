package ch.so.agi.dbeaver.ai.config;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class AiSettingsService {
    private static final Log LOG = Log.getLog(AiSettingsService.class);
    private static final String ENDPOINT_ID_SUFFIX = ".id";
    private static final String ENDPOINT_BASE_URL_SUFFIX = ".baseUrl";
    private static final String ENDPOINT_MODELS_SUFFIX = ".models";

    public AiSettings loadSettings() {
        return loadSettings(preferenceStore());
    }

    AiSettings loadSettings(DBPPreferenceStore store) {
        return new AiSettings(
            loadUserEndpoints(store),
            getPreferenceString(store, AiPreferenceConstants.PREF_SYSTEM_PROMPT, AiSettings.DEFAULT_SYSTEM_PROMPT),
            getPreferenceInt(store, AiPreferenceConstants.PREF_SAMPLE_ROW_LIMIT, AiSettings.DEFAULT_SAMPLE_ROW_LIMIT),
            getPreferenceInt(store, AiPreferenceConstants.PREF_MAX_REFERENCED_TABLES, AiSettings.DEFAULT_MAX_REFERENCED_TABLES),
            getPreferenceInt(store, AiPreferenceConstants.PREF_MAX_COLUMNS_PER_SAMPLE, AiSettings.DEFAULT_MAX_COLUMNS_PER_SAMPLE),
            getPreferenceBoolean(store, AiPreferenceConstants.PREF_INCLUDE_DDL, AiSettings.DEFAULT_INCLUDE_DDL),
            false,
            getPreferenceInt(store, AiPreferenceConstants.PREF_HISTORY_SIZE, AiSettings.DEFAULT_HISTORY_SIZE),
            getPreferenceInt(store, AiPreferenceConstants.PREF_MAX_CONTEXT_TOKENS, AiSettings.DEFAULT_MAX_CONTEXT_TOKENS),
            getPreferenceInt(store, AiPreferenceConstants.PREF_MENTION_PROPOSAL_LIMIT, AiSettings.DEFAULT_MENTION_PROPOSAL_LIMIT),
            getPreferenceInt(store, AiPreferenceConstants.PREF_MENTION_CANDIDATE_LIMIT, AiSettings.DEFAULT_MENTION_CANDIDATE_LIMIT),
            parseLlmLogMode(getPreferenceString(store, AiPreferenceConstants.PREF_LLM_LOG_MODE, AiSettings.DEFAULT_LLM_LOG_MODE.name())),
            getPreferenceBoolean(store, AiPreferenceConstants.PREF_LANGCHAIN_HTTP_LOGGING, AiSettings.DEFAULT_LANGCHAIN_HTTP_LOGGING),
            parseDoubleOrDefault(store.getString(AiPreferenceConstants.PREF_TEMPERATURE), AiSettings.DEFAULT_TEMPERATURE),
            getPreferenceIntInRangeOrDefault(
                store,
                AiPreferenceConstants.PREF_TIMEOUT_SECONDS,
                AiSettings.DEFAULT_TIMEOUT_SECONDS,
                AiSettings.MIN_TIMEOUT_SECONDS,
                AiSettings.MAX_TIMEOUT_SECONDS
            )
        );
    }

    private List<LlmEndpointConfig> loadUserEndpoints(DBPPreferenceStore store) {
        int count = getPreferenceInt(store, AiPreferenceConstants.PREF_LLM_ENDPOINT_COUNT, 0);
        List<LlmEndpointConfig> endpoints = new ArrayList<>();
        Set<String> seenBaseUrls = new LinkedHashSet<>();

        for (int i = 0; i < count; i++) {
            String id = trimToEmpty(store.getString(endpointKey(i, ENDPOINT_ID_SUFFIX)));
            String baseUrl = trimToEmpty(store.getString(endpointKey(i, ENDPOINT_BASE_URL_SUFFIX)));
            if (id.isEmpty() || baseUrl.isEmpty()) {
                continue;
            }
            String dedupe = baseUrl.toLowerCase();
            if (seenBaseUrls.contains(dedupe)) {
                LOG.warn("Ignoring duplicate LLM endpoint base URL: " + baseUrl);
                continue;
            }
            seenBaseUrls.add(dedupe);
            List<String> models = parseModelsCsv(store.getString(endpointKey(i, ENDPOINT_MODELS_SUFFIX)));
            endpoints.add(LlmEndpointConfig.user(id, baseUrl, models));
        }

        return endpoints;
    }

    private String endpointKey(int index, String suffix) {
        return AiPreferenceConstants.PREF_LLM_ENDPOINT_ID_PREFIX + index + suffix;
    }

    private String getPreferenceString(DBPPreferenceStore store, String key, String defaultValue) {
        String value = store.getString(key);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    private int getPreferenceInt(DBPPreferenceStore store, String key, int defaultValue) {
        int value = store.getInt(key);
        return (value == 0) ? defaultValue : value;
    }

    private int getPreferenceIntInRangeOrDefault(
        DBPPreferenceStore store,
        String key,
        int defaultValue,
        int minValue,
        int maxValue
    ) {
        String raw = store.getString(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            if (value < minValue || value > maxValue) {
                return defaultValue;
            }
            return value;
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }

    private boolean getPreferenceBoolean(DBPPreferenceStore store, String key, boolean defaultValue) {
        String stringValue = store.getString(key);
        if (stringValue == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(stringValue.trim());
    }

    public void saveSettings(AiSettings settings) {
        saveSettings(preferenceStore(), settings);
    }

    void saveSettings(DBPPreferenceStore store, AiSettings settings) {
        store.setValue(AiPreferenceConstants.PREF_SYSTEM_PROMPT, settings.systemPrompt());

        store.setValue(AiPreferenceConstants.PREF_SAMPLE_ROW_LIMIT, settings.sampleRowLimit());
        store.setValue(AiPreferenceConstants.PREF_MAX_REFERENCED_TABLES, settings.maxReferencedTables());
        store.setValue(AiPreferenceConstants.PREF_MAX_COLUMNS_PER_SAMPLE, settings.maxColumnsPerSample());

        store.setValue(AiPreferenceConstants.PREF_INCLUDE_DDL, settings.includeDdl());
        store.setValue(AiPreferenceConstants.PREF_INCLUDE_SAMPLE_ROWS, false);

        store.setValue(AiPreferenceConstants.PREF_HISTORY_SIZE, settings.historySize());
        store.setValue(AiPreferenceConstants.PREF_MAX_CONTEXT_TOKENS, settings.maxContextTokens());
        store.setValue(AiPreferenceConstants.PREF_MENTION_PROPOSAL_LIMIT, settings.mentionProposalLimit());
        store.setValue(AiPreferenceConstants.PREF_MENTION_CANDIDATE_LIMIT, settings.mentionCandidateLimit());
        store.setValue(AiPreferenceConstants.PREF_LLM_LOG_MODE, settings.llmLogMode().name());
        store.setValue(AiPreferenceConstants.PREF_LANGCHAIN_HTTP_LOGGING, settings.langchainHttpLogging());
        store.setValue(AiPreferenceConstants.PREF_TEMPERATURE, Double.toString(settings.temperature()));
        store.setValue(AiPreferenceConstants.PREF_TIMEOUT_SECONDS, settings.timeoutSeconds());

        List<LlmEndpointConfig> userEndpoints = settings.endpoints();
        int previousCount = getPreferenceInt(store, AiPreferenceConstants.PREF_LLM_ENDPOINT_COUNT, 0);
        store.setValue(AiPreferenceConstants.PREF_LLM_ENDPOINT_COUNT, userEndpoints.size());
        for (int i = 0; i < userEndpoints.size(); i++) {
            LlmEndpointConfig endpoint = userEndpoints.get(i);
            store.setValue(endpointKey(i, ENDPOINT_ID_SUFFIX), endpoint.id());
            store.setValue(endpointKey(i, ENDPOINT_BASE_URL_SUFFIX), endpoint.baseUrl());
            store.setValue(endpointKey(i, ENDPOINT_MODELS_SUFFIX), String.join(",", endpoint.models()));
        }
        for (int i = userEndpoints.size(); i < previousCount; i++) {
            store.setValue(endpointKey(i, ENDPOINT_ID_SUFFIX), "");
            store.setValue(endpointKey(i, ENDPOINT_BASE_URL_SUFFIX), "");
            store.setValue(endpointKey(i, ENDPOINT_MODELS_SUFFIX), "");
        }

        try {
            store.save();
        } catch (IOException e) {
            LOG.warn("Failed to save AI preference store", e);
        }
    }

    public String loadApiToken(String endpointId) {
        try {
            DBSSecretController controller = secretController();
            if (controller == null) {
                return "";
            }
            String token = controller.getPrivateSecretValue(secretKeyForEndpoint(endpointId));
            return token == null ? "" : token;
        } catch (DBException e) {
            LOG.warn("Failed to load API token from secret storage", e);
            return "";
        }
    }

    public void saveApiToken(String endpointId, String token) {
        try {
            DBSSecretController controller = secretController();
            if (controller == null) {
                LOG.warn("No secret controller available, API token not persisted");
                return;
            }
            controller.setPrivateSecretValue(secretKeyForEndpoint(endpointId), token == null ? "" : token);
            controller.flushChanges();
        } catch (DBException e) {
            LOG.warn("Failed to store API token in secret storage", e);
        }
    }

    public void deleteApiToken(String endpointId) {
        saveApiToken(endpointId, "");
    }

    public boolean hasApiToken(String endpointId) {
        return !loadApiToken(endpointId).isBlank();
    }

    String secretKeyForEndpoint(String endpointId) {
        String normalizedId = trimToEmpty(endpointId);
        if (normalizedId.isEmpty() || AiSettings.BUILTIN_OPENAI_ENDPOINT_ID.equals(normalizedId)) {
            return AiPreferenceConstants.SECRET_OPENAI_API_TOKEN;
        }
        return "ch.so.agi.dbeaver.ai.endpoint." + sanitizeForSecretKey(normalizedId) + ".apiToken";
    }

    private String sanitizeForSecretKey(String endpointId) {
        StringBuilder normalized = new StringBuilder();
        for (char c : endpointId.toCharArray()) {
            if (Character.isLetterOrDigit(c) || c == '-' || c == '_') {
                normalized.append(c);
            } else {
                normalized.append('_');
            }
        }
        return normalized.toString();
    }

    private DBPPreferenceStore preferenceStore() {
        return DBWorkbench.getPlatform().getPreferenceStore();
    }

    private DBSSecretController secretController() throws DBException {
        DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
        if (workspace != null) {
            DBPProject project = workspace.getActiveProject();
            if (project != null) {
                return DBSSecretController.getProjectSecretController(project);
            }
        }
        return DBSSecretController.getGlobalSecretController();
    }

    private double parseDoubleOrDefault(String value, double fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(value.trim());
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    static List<String> parseModelsCsv(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        LinkedHashSet<String> models = new LinkedHashSet<>();
        String[] parts = raw.split(",");
        for (String part : parts) {
            String model = part == null ? "" : part.trim();
            if (!model.isEmpty()) {
                models.add(model);
            }
        }
        return List.copyOf(models);
    }

    private LlmLogMode parseLlmLogMode(String value) {
        return LlmLogMode.fromPreferenceValue(value);
    }
}
