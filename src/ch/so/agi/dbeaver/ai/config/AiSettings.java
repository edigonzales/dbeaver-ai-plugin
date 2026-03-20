package ch.so.agi.dbeaver.ai.config;

import ch.so.agi.dbeaver.ai.chat.ChatRequestOptions;

import java.time.Duration;
import java.util.Objects;

public final class AiSettings {
    public static final String DEFAULT_BASE_URL = "https://api.infomaniak.com/2/ai/103965/openai/v1/";
    public static final String DEFAULT_MODEL = "qwen3";
    public static final String DEFAULT_SYSTEM_PROMPT = "Du bist ein Datenbank-Assistent und hilfst primaer beim Entwerfen, Debuggen und Optimieren herausfordernder SQL-Abfragen. Antworte immer auf Deutsch. Wenn du eine SQL-Abfrage lieferst, MUSS sie in einem ```sql```-Codeblock stehen. Zu jeder SQL-Abfrage MUSS eine kurze Erklaerung mitgeliefert werden (Zweck, zentrale Join-/Filter-/Aggregationslogik, Annahmen). Nutze bereitgestellten Tabellenkontext, insbesondere DDL, vorrangig und nenne fehlende Informationen explizit.";
    public static final int DEFAULT_SAMPLE_ROW_LIMIT = 5;
    public static final int DEFAULT_MAX_REFERENCED_TABLES = 8;
    public static final int DEFAULT_MAX_COLUMNS_PER_SAMPLE = 30;
    public static final int DEFAULT_HISTORY_SIZE = 12;
    public static final int DEFAULT_MAX_CONTEXT_TOKENS = 4_000;
    public static final double DEFAULT_TEMPERATURE = 0.0;
    public static final boolean DEFAULT_INCLUDE_DDL = true;
    public static final boolean DEFAULT_INCLUDE_SAMPLE_ROWS = false;
    public static final int DEFAULT_MENTION_PROPOSAL_LIMIT = 40;
    public static final int DEFAULT_MENTION_CANDIDATE_LIMIT = 100_000;
    public static final int DEFAULT_TIMEOUT_SECONDS = 90;
    public static final int MIN_TIMEOUT_SECONDS = 10;
    public static final int MAX_TIMEOUT_SECONDS = 600;
    public static final LlmLogMode DEFAULT_LLM_LOG_MODE = LlmLogMode.METADATA;
    public static final boolean DEFAULT_LANGCHAIN_HTTP_LOGGING = false;

    private final String baseUrl;
    private final String model;
    private final String systemPrompt;
    private final int sampleRowLimit;
    private final int maxReferencedTables;
    private final int maxColumnsPerSample;
    private final boolean includeDdl;
    private final boolean includeSampleRows;
    private final int historySize;
    private final int maxContextTokens;
    private final int mentionProposalLimit;
    private final int mentionCandidateLimit;
    private final int timeoutSeconds;
    private final LlmLogMode llmLogMode;
    private final boolean langchainHttpLogging;
    private final double temperature;

    public AiSettings(
        String baseUrl,
        String model,
        String systemPrompt,
        int sampleRowLimit,
        int maxReferencedTables,
        int maxColumnsPerSample,
        boolean includeDdl,
        boolean includeSampleRows,
        int historySize,
        int maxContextTokens,
        int mentionProposalLimit,
        int mentionCandidateLimit,
        LlmLogMode llmLogMode,
        boolean langchainHttpLogging,
        double temperature,
        int timeoutSeconds
    ) {
        this.baseUrl = normalizeOrDefault(baseUrl, DEFAULT_BASE_URL);
        this.model = normalizeOrDefault(model, DEFAULT_MODEL);
        this.systemPrompt = normalizeOrDefault(systemPrompt, DEFAULT_SYSTEM_PROMPT);
        this.sampleRowLimit = Math.max(1, sampleRowLimit);
        this.maxReferencedTables = Math.max(1, maxReferencedTables);
        this.maxColumnsPerSample = Math.max(1, maxColumnsPerSample);
        this.includeDdl = includeDdl;
        this.includeSampleRows = normalizeIncludeSampleRows(includeSampleRows);
        this.historySize = Math.max(0, historySize);
        this.maxContextTokens = Math.max(100, maxContextTokens);
        this.mentionProposalLimit = Math.max(1, mentionProposalLimit);
        this.mentionCandidateLimit = Math.max(1, mentionCandidateLimit);
        this.timeoutSeconds = clampTimeoutSeconds(timeoutSeconds);
        this.llmLogMode = llmLogMode == null ? DEFAULT_LLM_LOG_MODE : llmLogMode;
        this.langchainHttpLogging = langchainHttpLogging;
        this.temperature = clampTemperature(temperature);
    }

    private static String normalizeOrDefault(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private static double clampTemperature(double value) {
        if (Double.isNaN(value)) {
            return DEFAULT_TEMPERATURE;
        }
        return Math.max(0.0, Math.min(2.0, value));
    }

    private static int clampTimeoutSeconds(int value) {
        return Math.max(MIN_TIMEOUT_SECONDS, Math.min(MAX_TIMEOUT_SECONDS, value));
    }

    private static boolean normalizeIncludeSampleRows(boolean ignored) {
        return false;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String model() {
        return model;
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public int sampleRowLimit() {
        return sampleRowLimit;
    }

    public int maxReferencedTables() {
        return maxReferencedTables;
    }

    public int maxColumnsPerSample() {
        return maxColumnsPerSample;
    }

    public boolean includeDdl() {
        return includeDdl;
    }

    public boolean includeSampleRows() {
        return includeSampleRows;
    }

    public int historySize() {
        return historySize;
    }

    public int maxContextTokens() {
        return maxContextTokens;
    }

    public int mentionProposalLimit() {
        return mentionProposalLimit;
    }

    public int mentionCandidateLimit() {
        return mentionCandidateLimit;
    }

    public LlmLogMode llmLogMode() {
        return llmLogMode;
    }

    public boolean langchainHttpLogging() {
        return langchainHttpLogging;
    }

    public double temperature() {
        return temperature;
    }

    public int timeoutSeconds() {
        return timeoutSeconds;
    }

    public Duration timeout() {
        return Duration.ofSeconds(timeoutSeconds);
    }

    public ChatRequestOptions toChatRequestOptions() {
        int effectiveSampleRowLimit = includeSampleRows ? sampleRowLimit : DEFAULT_SAMPLE_ROW_LIMIT;
        int effectiveMaxColumnsPerSample = includeSampleRows ? maxColumnsPerSample : DEFAULT_MAX_COLUMNS_PER_SAMPLE;
        return new ChatRequestOptions(
            maxReferencedTables,
            effectiveSampleRowLimit,
            effectiveMaxColumnsPerSample,
            includeDdl,
            includeSampleRows,
            maxContextTokens,
            historySize
        );
    }

    public AiSettings withTemperature(double value) {
        return new AiSettings(
            baseUrl,
            model,
            systemPrompt,
            sampleRowLimit,
            maxReferencedTables,
            maxColumnsPerSample,
            includeDdl,
            includeSampleRows,
            historySize,
            maxContextTokens,
            mentionProposalLimit,
            mentionCandidateLimit,
            llmLogMode,
            langchainHttpLogging,
            value,
            timeoutSeconds
        );
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            baseUrl,
            model,
            systemPrompt,
            sampleRowLimit,
            maxReferencedTables,
            maxColumnsPerSample,
            includeDdl,
            includeSampleRows,
            historySize,
            maxContextTokens,
            mentionProposalLimit,
            mentionCandidateLimit,
            timeoutSeconds,
            llmLogMode,
            langchainHttpLogging,
            temperature
        );
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof AiSettings other)) {
            return false;
        }
        return sampleRowLimit == other.sampleRowLimit
            && maxReferencedTables == other.maxReferencedTables
            && maxColumnsPerSample == other.maxColumnsPerSample
            && includeDdl == other.includeDdl
            && includeSampleRows == other.includeSampleRows
            && historySize == other.historySize
            && maxContextTokens == other.maxContextTokens
            && mentionProposalLimit == other.mentionProposalLimit
            && mentionCandidateLimit == other.mentionCandidateLimit
            && timeoutSeconds == other.timeoutSeconds
            && llmLogMode == other.llmLogMode
            && langchainHttpLogging == other.langchainHttpLogging
            && Double.compare(temperature, other.temperature) == 0
            && baseUrl.equals(other.baseUrl)
            && model.equals(other.model)
            && systemPrompt.equals(other.systemPrompt);
    }
}
