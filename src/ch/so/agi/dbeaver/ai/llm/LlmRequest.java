package ch.so.agi.dbeaver.ai.llm;

import ch.so.agi.dbeaver.ai.model.ChatMessage;

import java.util.List;
import java.util.Objects;

public final class LlmRequest {
    private final String systemPrompt;
    private final String userPrompt;
    private final String contextBlock;
    private final List<ChatMessage> history;

    public LlmRequest(String systemPrompt, String userPrompt, String contextBlock, List<ChatMessage> history) {
        this.systemPrompt = Objects.requireNonNull(systemPrompt, "systemPrompt");
        this.userPrompt = Objects.requireNonNull(userPrompt, "userPrompt");
        this.contextBlock = contextBlock == null ? "" : contextBlock;
        this.history = List.copyOf(Objects.requireNonNull(history, "history"));
    }

    public String systemPrompt() {
        return systemPrompt;
    }

    public String userPrompt() {
        return userPrompt;
    }

    public String contextBlock() {
        return contextBlock;
    }

    public List<ChatMessage> history() {
        return history;
    }
}
