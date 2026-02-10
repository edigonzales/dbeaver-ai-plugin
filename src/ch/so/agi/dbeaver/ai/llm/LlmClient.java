package ch.so.agi.dbeaver.ai.llm;

public interface LlmClient {
    StreamingHandle stream(LlmRequest request, LlmResponseListener listener);
}
