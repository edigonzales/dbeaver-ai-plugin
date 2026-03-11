package ch.so.agi.dbeaver.ai.chat;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatSessionTest {
    @Test
    void clearRemovesAllMessagesFromSnapshot() {
        ChatSession session = new ChatSession();
        session.addUser("Frage");
        session.addAssistant("Antwort");

        session.clear();

        assertThat(session.snapshot()).isEmpty();
    }

    @Test
    void clearRemovesAllMessagesFromRecentHistory() {
        ChatSession session = new ChatSession();
        session.addUser("Frage");
        session.addAssistant("Antwort");

        session.clear();

        assertThat(session.recentHistory(10)).isEmpty();
    }
}
