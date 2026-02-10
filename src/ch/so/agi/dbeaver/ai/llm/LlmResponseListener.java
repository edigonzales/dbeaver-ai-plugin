package ch.so.agi.dbeaver.ai.llm;

public interface LlmResponseListener {
    void onPartialText(String textChunk);

    void onComplete(String finalText);

    void onError(Throwable error);
}
