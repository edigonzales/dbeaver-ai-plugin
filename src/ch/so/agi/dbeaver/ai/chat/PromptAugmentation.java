package ch.so.agi.dbeaver.ai.chat;

import java.util.List;

public record PromptAugmentation(
    String rawUserPrompt,
    String normalizedUserPrompt,
    String sqlContextBlock,
    List<String> warnings
) {
    public PromptAugmentation {
        rawUserPrompt = rawUserPrompt == null ? "" : rawUserPrompt;
        normalizedUserPrompt = normalizedUserPrompt == null ? "" : normalizedUserPrompt;
        sqlContextBlock = sqlContextBlock == null ? "" : sqlContextBlock;
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public static PromptAugmentation raw(String rawUserPrompt) {
        String safeRaw = rawUserPrompt == null ? "" : rawUserPrompt;
        return new PromptAugmentation(safeRaw, safeRaw, "", List.of());
    }
}
