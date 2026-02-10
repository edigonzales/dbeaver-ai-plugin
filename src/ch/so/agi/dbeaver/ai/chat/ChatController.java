package ch.so.agi.dbeaver.ai.chat;

import ch.so.agi.dbeaver.ai.context.ContextAssembler;
import ch.so.agi.dbeaver.ai.context.ContextEnricher;
import ch.so.agi.dbeaver.ai.llm.ContextAwarePromptComposer;
import ch.so.agi.dbeaver.ai.llm.LlmClient;
import ch.so.agi.dbeaver.ai.llm.LlmRequest;
import ch.so.agi.dbeaver.ai.llm.LlmResponseListener;
import ch.so.agi.dbeaver.ai.llm.StreamingHandle;
import ch.so.agi.dbeaver.ai.mention.MentionParser;
import ch.so.agi.dbeaver.ai.model.ChatMessage;
import ch.so.agi.dbeaver.ai.model.ContextBundle;
import ch.so.agi.dbeaver.ai.model.TableReference;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public final class ChatController {
    private final ChatSession session;
    private final MentionParser mentionParser;
    private final ContextEnricher contextEnricher;
    private final ContextAssembler contextAssembler;
    private final ContextAwarePromptComposer promptComposer;
    private final LlmClient llmClient;

    private final AtomicReference<StreamingHandle> activeStream = new AtomicReference<>();

    public ChatController(
        ChatSession session,
        MentionParser mentionParser,
        ContextEnricher contextEnricher,
        ContextAssembler contextAssembler,
        ContextAwarePromptComposer promptComposer,
        LlmClient llmClient
    ) {
        this.session = Objects.requireNonNull(session, "session");
        this.mentionParser = Objects.requireNonNull(mentionParser, "mentionParser");
        this.contextEnricher = Objects.requireNonNull(contextEnricher, "contextEnricher");
        this.contextAssembler = Objects.requireNonNull(contextAssembler, "contextAssembler");
        this.promptComposer = Objects.requireNonNull(promptComposer, "promptComposer");
        this.llmClient = Objects.requireNonNull(llmClient, "llmClient");
    }

    public void cancelActiveRequest() {
        StreamingHandle handle = activeStream.getAndSet(null);
        if (handle != null) {
            handle.cancel();
        }
    }

    public void send(
        String systemPrompt,
        String userPrompt,
        ChatRequestOptions options,
        ChatUiListener listener
    ) {
        Objects.requireNonNull(listener, "listener");
        Objects.requireNonNull(options, "options");

        cancelActiveRequest();

        List<ChatMessage> historyBeforeCurrent = session.recentHistory(options.historySize());

        listener.onBeforeSend(userPrompt);
        session.addUser(userPrompt);

        List<TableReference> references = mentionParser.parseReferences(userPrompt);
        ContextBundle contextBundle = contextEnricher.build(
            references,
            options.maxReferencedTables(),
            options.sampleRowLimit(),
            options.maxColumnsPerSample(),
            options.includeDdl(),
            options.includeSampleRows()
        );

        ContextBundle boundedContext = contextAssembler.truncateToBudget(contextBundle, options.maxContextTokens());
        String promptBlock = contextAssembler.toPromptBlock(boundedContext);

        listener.onContextBuilt(boundedContext, promptBlock);
        for (String warning : boundedContext.warnings()) {
            listener.onWarning(warning);
        }

        String composedUserPrompt = promptComposer.composeUserPrompt(userPrompt, promptBlock);

        LlmRequest llmRequest = new LlmRequest(
            systemPrompt,
            composedUserPrompt,
            promptBlock,
            historyBeforeCurrent
        );

        StringBuilder assistantBuffer = new StringBuilder();

        StreamingHandle handle = llmClient.stream(llmRequest, new LlmResponseListener() {
            @Override
            public void onPartialText(String textChunk) {
                assistantBuffer.append(textChunk);
                listener.onAssistantPartial(textChunk);
            }

            @Override
            public void onComplete(String finalText) {
                activeStream.set(null);
                String effectiveText = finalText == null || finalText.isBlank() ? assistantBuffer.toString() : finalText;
                session.addAssistant(effectiveText);
                listener.onAssistantComplete(effectiveText);
            }

            @Override
            public void onError(Throwable error) {
                activeStream.set(null);
                listener.onError("LLM request failed", error);
            }
        });

        activeStream.set(handle);
    }
}
