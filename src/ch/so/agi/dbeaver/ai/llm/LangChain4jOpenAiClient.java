package ch.so.agi.dbeaver.ai.llm;

import ch.so.agi.dbeaver.ai.model.ChatMessage;
import ch.so.agi.dbeaver.ai.model.ChatRole;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.PartialResponse;
import dev.langchain4j.model.chat.response.PartialResponseContext;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import org.jkiss.dbeaver.Log;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class LangChain4jOpenAiClient implements LlmClient {
    private static final Log LOG = Log.getLog(LangChain4jOpenAiClient.class);

    private final String baseUrl;
    private final String apiKey;
    private final String modelName;
    private final double temperature;
    private final Duration timeout;

    public LangChain4jOpenAiClient(
        String baseUrl,
        String apiKey,
        String modelName,
        double temperature,
        Duration timeout
    ) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl");
        this.apiKey = Objects.requireNonNull(apiKey, "apiKey");
        this.modelName = Objects.requireNonNull(modelName, "modelName");
        this.temperature = temperature;
        this.timeout = Objects.requireNonNull(timeout, "timeout");
    }

    @Override
    public StreamingHandle stream(LlmRequest request, LlmResponseListener listener) {
        Objects.requireNonNull(request, "request");
        Objects.requireNonNull(listener, "listener");

        OpenAiStreamingChatModel model = OpenAiStreamingChatModel.builder()
            .baseUrl(baseUrl)
            .apiKey(apiKey)
            .modelName(modelName)
            .temperature(temperature)
            .timeout(timeout)
            .logRequests(true)
            .logResponses(true)
            .build();

        List<dev.langchain4j.data.message.ChatMessage> messages = new ArrayList<>();
        messages.add(SystemMessage.from(request.systemPrompt()));

        for (ChatMessage msg : request.history()) {
            if (msg.role() == ChatRole.USER) {
                messages.add(UserMessage.from(msg.text()));
            } else if (msg.role() == ChatRole.ASSISTANT) {
                messages.add(AiMessage.from(msg.text()));
            }
        }

        messages.add(UserMessage.from(request.userPrompt()));

        ChatRequest chatRequest = ChatRequest.builder()
            .messages(messages)
            .build();

        LOG.info(
            "LLM request started (provider=langchain4j-openai, model=" + modelName
                + ", baseUrl=" + baseUrl
                + ", historyMessages=" + request.history().size()
                + ", userPromptChars=" + request.userPrompt().length()
                + ", contextChars=" + request.contextBlock().length()
                + ")"
        );

        AtomicBoolean cancelled = new AtomicBoolean(false);
        AtomicReference<dev.langchain4j.model.chat.response.StreamingHandle> modelHandleRef = new AtomicReference<>();
        StringBuilder partialText = new StringBuilder();

        model.chat(chatRequest, new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(PartialResponse partialResponse, PartialResponseContext context) {
                modelHandleRef.compareAndSet(null, context.streamingHandle());
                if (cancelled.get()) {
                    context.streamingHandle().cancel();
                    return;
                }

                String chunk = partialResponse.text();
                partialText.append(chunk);
                listener.onPartialText(chunk);
            }

            @Override
            public void onCompleteResponse(ChatResponse completeResponse) {
                if (cancelled.get()) {
                    return;
                }

                String text = completeResponse.aiMessage() == null ? "" : completeResponse.aiMessage().text();
                if (text == null || text.isEmpty()) {
                    text = partialText.toString();
                }
                LOG.info("LLM response completed (chars=" + text.length() + ")");
                listener.onComplete(text);
            }

            @Override
            public void onError(Throwable error) {
                if (cancelled.get()) {
                    return;
                }
                LOG.error("LLM request failed", error);
                listener.onError(error);
            }
        });

        return () -> {
            cancelled.set(true);
            dev.langchain4j.model.chat.response.StreamingHandle handle = modelHandleRef.get();
            if (handle != null) {
                handle.cancel();
            }
        };
    }
}
