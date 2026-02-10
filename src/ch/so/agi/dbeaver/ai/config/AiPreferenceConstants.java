package ch.so.agi.dbeaver.ai.config;

public final class AiPreferenceConstants {
    private AiPreferenceConstants() {
    }

    public static final String PREF_SYSTEM_PROMPT = "ch.so.agi.dbeaver.ai.systemPrompt";
    public static final String PREF_MODEL = "ch.so.agi.dbeaver.ai.model";
    public static final String PREF_BASE_URL = "ch.so.agi.dbeaver.ai.baseUrl";
    public static final String PREF_SAMPLE_ROW_LIMIT = "ch.so.agi.dbeaver.ai.sampleRowLimit";
    public static final String PREF_MAX_REFERENCED_TABLES = "ch.so.agi.dbeaver.ai.maxReferencedTables";
    public static final String PREF_MAX_COLUMNS_PER_SAMPLE = "ch.so.agi.dbeaver.ai.maxColumnsPerSample";
    public static final String PREF_INCLUDE_DDL = "ch.so.agi.dbeaver.ai.includeDdl";
    public static final String PREF_INCLUDE_SAMPLE_ROWS = "ch.so.agi.dbeaver.ai.includeSampleRows";

    public static final String PREF_HISTORY_SIZE = "ch.so.agi.dbeaver.ai.historySize";
    public static final String PREF_MAX_CONTEXT_TOKENS = "ch.so.agi.dbeaver.ai.maxContextTokens";
    public static final String PREF_MENTION_PROPOSAL_LIMIT = "ch.so.agi.dbeaver.ai.mentionProposalLimit";
    public static final String PREF_TEMPERATURE = "ch.so.agi.dbeaver.ai.temperature";
    public static final String PREF_LLM_LOG_MODE = "ch.so.agi.dbeaver.ai.llmLogMode";
    public static final String PREF_LANGCHAIN_HTTP_LOGGING = "ch.so.agi.dbeaver.ai.langchainHttpLogging";

    public static final String SECRET_OPENAI_API_TOKEN = "ch.so.agi.dbeaver.ai.openai.apiToken";
}
