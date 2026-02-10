package ch.so.agi.dbeaver.ai.chat;

import ch.so.agi.dbeaver.ai.model.ContextBundle;

public interface ChatUiListener {
    void onBeforeSend(String userPrompt);

    void onContextBuilt(ContextBundle contextBundle, String promptBlock);

    void onAssistantPartial(String chunk);

    void onAssistantComplete(String finalText);

    void onWarning(String warning);

    void onError(String message, Throwable error);
}
