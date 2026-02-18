package ch.so.agi.dbeaver.ai.config;

import org.eclipse.core.runtime.preferences.AbstractPreferenceInitializer;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;

public final class AiPreferenceInitializer extends AbstractPreferenceInitializer {
    private static final Log LOG = Log.getLog(AiPreferenceInitializer.class);

    @Override
    public void initializeDefaultPreferences() {
        try {
            DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

            store.setDefault(AiPreferenceConstants.PREF_BASE_URL, AiSettings.DEFAULT_BASE_URL);
            store.setDefault(AiPreferenceConstants.PREF_MODEL, AiSettings.DEFAULT_MODEL);
            store.setDefault(AiPreferenceConstants.PREF_SYSTEM_PROMPT, AiSettings.DEFAULT_SYSTEM_PROMPT);

            store.setDefault(AiPreferenceConstants.PREF_SAMPLE_ROW_LIMIT, 5);
            store.setDefault(AiPreferenceConstants.PREF_MAX_REFERENCED_TABLES, 8);
            store.setDefault(AiPreferenceConstants.PREF_MAX_COLUMNS_PER_SAMPLE, 30);

            store.setDefault(AiPreferenceConstants.PREF_INCLUDE_DDL, AiSettings.DEFAULT_INCLUDE_DDL);
            store.setDefault(AiPreferenceConstants.PREF_INCLUDE_SAMPLE_ROWS, AiSettings.DEFAULT_INCLUDE_SAMPLE_ROWS);

            store.setDefault(AiPreferenceConstants.PREF_HISTORY_SIZE, 12);
            store.setDefault(AiPreferenceConstants.PREF_MAX_CONTEXT_TOKENS, 4_000);
            store.setDefault(AiPreferenceConstants.PREF_MENTION_PROPOSAL_LIMIT, AiSettings.DEFAULT_MENTION_PROPOSAL_LIMIT);
            store.setDefault(AiPreferenceConstants.PREF_MENTION_CANDIDATE_LIMIT, AiSettings.DEFAULT_MENTION_CANDIDATE_LIMIT);
            store.setDefault(AiPreferenceConstants.PREF_TEMPERATURE, 0.0);
            store.setDefault(AiPreferenceConstants.PREF_LLM_LOG_MODE, AiSettings.DEFAULT_LLM_LOG_MODE.name());
            store.setDefault(AiPreferenceConstants.PREF_LANGCHAIN_HTTP_LOGGING, AiSettings.DEFAULT_LANGCHAIN_HTTP_LOGGING);

            LOG.debug("AI plugin default preferences initialized successfully");
        } catch (Exception e) {
            LOG.error("Failed to initialize AI plugin default preferences", e);
        }
    }
}
