package ch.so.agi.dbeaver.ai.config;

import ch.so.agi.dbeaver.ai.chat.ChatRequestOptions;

import java.time.Duration;
import java.util.Objects;

public final class AiSettings {
    public static final String DEFAULT_BASE_URL = "https://api.openai.com/v1";
    public static final String DEFAULT_MODEL = "gpt-4o-mini";
    public static final String DEFAULT_SYSTEM_PROMPT = "Du bist ein Datenbank-Assistent und hilfst primär beim Entwerfen, Debuggen und Optimieren herausfordernder SQL-Abfragen. Antworte immer auf Deutsch. Wenn du eine SQL-Abfrage lieferst, MUSS sie in einem ```sql```-Codeblock stehen. Zu jeder SQL-Abfrage MUSS eine kurze Erklärung mitgeliefert werden (Zweck, zentrale Join-/Filter-/Aggregationslogik, Annahmen). Nutze bereitgestellten Tabellenkontext (DDL und Sample Rows) vorrangig und nenne fehlende Informationen explizit.";
    public static final boolean DEFAULT_INCLUDE_DDL = true;
    public static final boolean DEFAULT_INCLUDE_SAMPLE_ROWS = true;
    public static final int DEFAULT_MENTION_PROPOSAL_LIMIT = 40;

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
        double temperature
    ) {
        this.baseUrl = normalizeOrDefault(baseUrl, DEFAULT_BASE_URL);
        this.model = normalizeOrDefault(model, DEFAULT_MODEL);
        this.systemPrompt = normalizeOrDefault(systemPrompt, DEFAULT_SYSTEM_PROMPT);
        this.sampleRowLimit = Math.max(1, sampleRowLimit);
        this.maxReferencedTables = Math.max(1, maxReferencedTables);
        this.maxColumnsPerSample = Math.max(1, maxColumnsPerSample);
        this.includeDdl = includeDdl;
        this.includeSampleRows = includeSampleRows;
        this.historySize = Math.max(0, historySize);
        this.maxContextTokens = Math.max(100, maxContextTokens);
        this.mentionProposalLimit = Math.max(1, mentionProposalLimit);
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
            return 0.0;
        }
        return Math.max(0.0, Math.min(2.0, value));
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

    public double temperature() {
        return temperature;
    }

    public Duration timeout() {
        return Duration.ofSeconds(90);
    }

    public ChatRequestOptions toChatRequestOptions() {
        return new ChatRequestOptions(
            maxReferencedTables,
            sampleRowLimit,
            maxColumnsPerSample,
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
            value
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
            && Double.compare(temperature, other.temperature) == 0
            && baseUrl.equals(other.baseUrl)
            && model.equals(other.model)
            && systemPrompt.equals(other.systemPrompt);
    }
}
