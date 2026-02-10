package ch.so.agi.dbeaver.ai.chat;

import ch.so.agi.dbeaver.ai.model.ChatMessage;
import ch.so.agi.dbeaver.ai.model.ChatRole;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ChatSession {
    private final List<ChatMessage> messages = new ArrayList<>();

    public synchronized ChatMessage addSystem(String text) {
        ChatMessage msg = new ChatMessage(ChatRole.SYSTEM, text, Instant.now());
        messages.add(msg);
        return msg;
    }

    public synchronized ChatMessage addUser(String text) {
        ChatMessage msg = new ChatMessage(ChatRole.USER, text, Instant.now());
        messages.add(msg);
        return msg;
    }

    public synchronized ChatMessage addAssistant(String text) {
        ChatMessage msg = new ChatMessage(ChatRole.ASSISTANT, text, Instant.now());
        messages.add(msg);
        return msg;
    }

    public synchronized List<ChatMessage> snapshot() {
        return Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public synchronized List<ChatMessage> recentHistory(int maxMessages) {
        if (maxMessages <= 0 || messages.isEmpty()) {
            return List.of();
        }
        int from = Math.max(0, messages.size() - maxMessages);
        return Collections.unmodifiableList(new ArrayList<>(messages.subList(from, messages.size())));
    }
}
