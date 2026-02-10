package ch.so.agi.dbeaver.ai.context;

public final class PromptBudgetEstimator {

    // Deliberately conservative rough estimation.
    private static final int CHARS_PER_TOKEN = 4;

    public int estimateTokens(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, (text.length() + CHARS_PER_TOKEN - 1) / CHARS_PER_TOKEN);
    }
}
