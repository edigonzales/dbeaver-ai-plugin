package ch.so.agi.dbeaver.ai.chat;

public record ChatRequestOptions(
    int maxReferencedTables,
    int sampleRowLimit,
    int maxColumnsPerSample,
    boolean includeDdl,
    boolean includeSampleRows,
    int maxContextTokens,
    int historySize
) {
}
