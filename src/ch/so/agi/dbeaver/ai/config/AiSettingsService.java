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
            store.getString(AiPreferenceConstants.PREF_BASE_URL),
            store.getString(AiPreferenceConstants.PREF_MODEL),
            store.getString(AiPreferenceConstants.PREF_SYSTEM_PROMPT),
            store.getInt(AiPreferenceConstants.PREF_SAMPLE_ROW_LIMIT),
            store.getInt(AiPreferenceConstants.PREF_MAX_REFERENCED_TABLES),
            store.getInt(AiPreferenceConstants.PREF_MAX_COLUMNS_PER_SAMPLE),
            store.getBoolean(AiPreferenceConstants.PREF_INCLUDE_DDL),
            store.getBoolean(AiPreferenceConstants.PREF_INCLUDE_SAMPLE_ROWS),
            store.getInt(AiPreferenceConstants.PREF_HISTORY_SIZE),
            store.getInt(AiPreferenceConstants.PREF_MAX_CONTEXT_TOKENS),
            store.getInt(AiPreferenceConstants.PREF_MENTION_PROPOSAL_LIMIT),
            parseDoubleOrDefault(store.getString(AiPreferenceConstants.PREF_TEMPERATURE), 0.0)
        );
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
}
