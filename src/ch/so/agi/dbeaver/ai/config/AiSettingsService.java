package ch.so.agi.dbeaver.ai.config;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.secret.DBSSecretController;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.io.IOException;

public final class AiSettingsService {
    private static final Log LOG = Log.getLog(AiSettingsService.class);

    public AiSettings loadSettings() {
        DBPPreferenceStore store = preferenceStore();
        return new AiSettings(
            getPreferenceString(store, AiPreferenceConstants.PREF_BASE_URL, AiSettings.DEFAULT_BASE_URL),
            getPreferenceString(store, AiPreferenceConstants.PREF_MODEL, AiSettings.DEFAULT_MODEL),
            getPreferenceString(store, AiPreferenceConstants.PREF_SYSTEM_PROMPT, AiSettings.DEFAULT_SYSTEM_PROMPT),
            getPreferenceInt(store, AiPreferenceConstants.PREF_SAMPLE_ROW_LIMIT, 5),
            getPreferenceInt(store, AiPreferenceConstants.PREF_MAX_REFERENCED_TABLES, 8),
            getPreferenceInt(store, AiPreferenceConstants.PREF_MAX_COLUMNS_PER_SAMPLE, 30),
            getPreferenceBoolean(store, AiPreferenceConstants.PREF_INCLUDE_DDL, AiSettings.DEFAULT_INCLUDE_DDL),
            getPreferenceBoolean(store, AiPreferenceConstants.PREF_INCLUDE_SAMPLE_ROWS, AiSettings.DEFAULT_INCLUDE_SAMPLE_ROWS),
            getPreferenceInt(store, AiPreferenceConstants.PREF_HISTORY_SIZE, 12),
            getPreferenceInt(store, AiPreferenceConstants.PREF_MAX_CONTEXT_TOKENS, 4_000),
            getPreferenceInt(store, AiPreferenceConstants.PREF_MENTION_PROPOSAL_LIMIT, AiSettings.DEFAULT_MENTION_PROPOSAL_LIMIT),
            getPreferenceInt(store, AiPreferenceConstants.PREF_MENTION_CANDIDATE_LIMIT, AiSettings.DEFAULT_MENTION_CANDIDATE_LIMIT),
            parseLlmLogMode(getPreferenceString(store, AiPreferenceConstants.PREF_LLM_LOG_MODE, AiSettings.DEFAULT_LLM_LOG_MODE.name())),
            getPreferenceBoolean(store, AiPreferenceConstants.PREF_LANGCHAIN_HTTP_LOGGING, AiSettings.DEFAULT_LANGCHAIN_HTTP_LOGGING),
            parseDoubleOrDefault(store.getString(AiPreferenceConstants.PREF_TEMPERATURE), 0.0)
        );
    }

    private String getPreferenceString(DBPPreferenceStore store, String key, String defaultValue) {
        String value = store.getString(key);
        return (value == null || value.isBlank()) ? defaultValue : value.trim();
    }

    private int getPreferenceInt(DBPPreferenceStore store, String key, int defaultValue) {
        // First try to get the value; if not set (returns 0), fall back to default
        int value = store.getInt(key);
        return (value == 0) ? defaultValue : value;
    }

    private boolean getPreferenceBoolean(DBPPreferenceStore store, String key, boolean defaultValue) {
        // Use store.getBoolean() which returns false for unset values
        // We need to check if the value was explicitly set
        String stringValue = store.getString(key);
        if (stringValue == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(stringValue.trim());
    }

    public void saveSettings(AiSettings settings) {
        DBPPreferenceStore store = preferenceStore();
        store.setValue(AiPreferenceConstants.PREF_BASE_URL, settings.baseUrl());
        store.setValue(AiPreferenceConstants.PREF_MODEL, settings.model());
        store.setValue(AiPreferenceConstants.PREF_SYSTEM_PROMPT, settings.systemPrompt());

        store.setValue(AiPreferenceConstants.PREF_SAMPLE_ROW_LIMIT, settings.sampleRowLimit());
        store.setValue(AiPreferenceConstants.PREF_MAX_REFERENCED_TABLES, settings.maxReferencedTables());
        store.setValue(AiPreferenceConstants.PREF_MAX_COLUMNS_PER_SAMPLE, settings.maxColumnsPerSample());

        store.setValue(AiPreferenceConstants.PREF_INCLUDE_DDL, settings.includeDdl());
        store.setValue(AiPreferenceConstants.PREF_INCLUDE_SAMPLE_ROWS, settings.includeSampleRows());

        store.setValue(AiPreferenceConstants.PREF_HISTORY_SIZE, settings.historySize());
        store.setValue(AiPreferenceConstants.PREF_MAX_CONTEXT_TOKENS, settings.maxContextTokens());
        store.setValue(AiPreferenceConstants.PREF_MENTION_PROPOSAL_LIMIT, settings.mentionProposalLimit());
        store.setValue(AiPreferenceConstants.PREF_MENTION_CANDIDATE_LIMIT, settings.mentionCandidateLimit());
        store.setValue(AiPreferenceConstants.PREF_LLM_LOG_MODE, settings.llmLogMode().name());
        store.setValue(AiPreferenceConstants.PREF_LANGCHAIN_HTTP_LOGGING, settings.langchainHttpLogging());
        store.setValue(AiPreferenceConstants.PREF_TEMPERATURE, Double.toString(settings.temperature()));

        try {
            store.save();
        } catch (IOException e) {
            LOG.warn("Failed to save AI preference store", e);
        }
    }

    public String loadApiToken() {
        try {
            DBSSecretController controller = secretController();
            if (controller == null) {
                return "";
            }
            String token = controller.getPrivateSecretValue(AiPreferenceConstants.SECRET_OPENAI_API_TOKEN);
            return token == null ? "" : token;
        } catch (DBException e) {
            LOG.warn("Failed to load API token from secret storage", e);
            return "";
        }
    }

    public void saveApiToken(String token) {
        try {
            DBSSecretController controller = secretController();
            if (controller == null) {
                LOG.warn("No secret controller available, API token not persisted");
                return;
            }
            controller.setPrivateSecretValue(AiPreferenceConstants.SECRET_OPENAI_API_TOKEN, token == null ? "" : token);
            controller.flushChanges();
        } catch (DBException e) {
            LOG.warn("Failed to store API token in secret storage", e);
        }
    }

    public boolean hasApiToken() {
        return !loadApiToken().isBlank();
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

    private LlmLogMode parseLlmLogMode(String value) {
        return LlmLogMode.fromPreferenceValue(value);
    }
}
