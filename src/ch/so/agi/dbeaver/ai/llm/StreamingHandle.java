package ch.so.agi.dbeaver.ai.llm;

@FunctionalInterface
public interface StreamingHandle {
    void cancel();
}
