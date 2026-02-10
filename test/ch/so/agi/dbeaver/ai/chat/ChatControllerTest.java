package ch.so.agi.dbeaver.ai.chat;

import ch.so.agi.dbeaver.ai.context.ContextAssembler;
import ch.so.agi.dbeaver.ai.context.ContextEnricher;
import ch.so.agi.dbeaver.ai.context.PromptBudgetEstimator;
import ch.so.agi.dbeaver.ai.context.SampleRowsCollector;
import ch.so.agi.dbeaver.ai.context.SensitiveDataMasker;
import ch.so.agi.dbeaver.ai.context.TableDdlExtractor;
import ch.so.agi.dbeaver.ai.context.TableReferenceResolver;
import ch.so.agi.dbeaver.ai.llm.ContextAwarePromptComposer;
import ch.so.agi.dbeaver.ai.llm.LlmClient;
import ch.so.agi.dbeaver.ai.llm.LlmRequest;
import ch.so.agi.dbeaver.ai.llm.LlmResponseListener;
import ch.so.agi.dbeaver.ai.llm.StreamingHandle;
import ch.so.agi.dbeaver.ai.mention.MentionParser;
import ch.so.agi.dbeaver.ai.model.ChatRole;
import ch.so.agi.dbeaver.ai.model.ContextBundle;
import ch.so.agi.dbeaver.ai.model.ResolvedTable;
import ch.so.agi.dbeaver.ai.model.ResolvedTableResult;
import ch.so.agi.dbeaver.ai.model.TableReference;
import ch.so.agi.dbeaver.ai.model.TableSampleRow;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ChatControllerTest {
    @Test
    void sendsPromptBuildsContextAndStoresAssistantReply() {
        ChatSession session = new ChatSession();
        MentionParser mentionParser = new MentionParser();

        TableReferenceResolver resolver = refs -> {
            List<ResolvedTable> resolved = new ArrayList<>();
            for (TableReference ref : refs) {
                resolved.add(new ResolvedTable(ref, ref.canonicalId(), new Object(), new Object()));
            }
            return new ResolvedTableResult(resolved, List.of());
        };

        TableDdlExtractor ddlExtractor = table -> "CREATE TABLE " + table.fullyQualifiedName() + "(id int);";

        SampleRowsCollector sampleRowsCollector = new SampleRowsCollector() {
            @Override
            public List<TableSampleRow> collect(ResolvedTable resolvedTable, int maxRows, int maxColumns) {
                Map<String, String> row = new LinkedHashMap<>();
                row.put("id", "1");
                return List.of(new TableSampleRow(row));
            }

            @Override
            public String createSampleQueryText(ResolvedTable resolvedTable, int maxRows) {
                return "SELECT * FROM " + resolvedTable.fullyQualifiedName() + " LIMIT " + maxRows + ";";
            }
        };

        ContextEnricher enricher = new ContextEnricher(resolver, ddlExtractor, sampleRowsCollector, new SensitiveDataMasker());
        ContextAssembler assembler = new ContextAssembler(new PromptBudgetEstimator());
        ContextAwarePromptComposer composer = new ContextAwarePromptComposer();

        LlmClient llmClient = (request, listener) -> {
            listener.onPartialText("Hello");
            listener.onComplete("Hello world");
            return () -> {
            };
        };

        ChatController controller = new ChatController(session, mentionParser, enricher, assembler, composer, llmClient);
        RecordingUiListener ui = new RecordingUiListener();

        controller.send(
            "system",
            "Analyse #db.s.users",
            new ChatRequestOptions(8, 5, 30, true, true, 2_000, 10),
            ui
        );

        assertThat(ui.beforeSendText).isEqualTo("Analyse #db.s.users");
        assertThat(ui.partialChunks).contains("Hello");
        assertThat(ui.completedText).isEqualTo("Hello world");
        assertThat(ui.promptBlock).contains("### Tabelle: db.s.users (Mention: #db.s.users)");

        assertThat(session.snapshot()).hasSize(2);
        assertThat(session.snapshot().get(0).role()).isEqualTo(ChatRole.USER);
        assertThat(session.snapshot().get(1).role()).isEqualTo(ChatRole.ASSISTANT);
    }

    @Test
    void cancelStopsActiveStream() {
        ChatSession session = new ChatSession();

        ContextEnricher enricher = new ContextEnricher(
            refs -> new ResolvedTableResult(List.of(), List.of()),
            table -> "",
            new SampleRowsCollector() {
                @Override
                public List<TableSampleRow> collect(ResolvedTable resolvedTable, int maxRows, int maxColumns) {
                    return List.of();
                }

                @Override
                public String createSampleQueryText(ResolvedTable resolvedTable, int maxRows) {
                    return "";
                }
            },
            new SensitiveDataMasker()
        );

        AtomicBoolean cancelled = new AtomicBoolean(false);
        LlmClient llmClient = new LlmClient() {
            @Override
            public StreamingHandle stream(LlmRequest request, LlmResponseListener listener) {
                return () -> cancelled.set(true);
            }
        };

        ChatController controller = new ChatController(
            session,
            new MentionParser(),
            enricher,
            new ContextAssembler(new PromptBudgetEstimator()),
            new ContextAwarePromptComposer(),
            llmClient
        );

        controller.send(
            "system",
            "hello",
            new ChatRequestOptions(1, 1, 1, false, false, 1000, 0),
            new RecordingUiListener()
        );

        controller.cancelActiveRequest();

        assertThat(cancelled).isTrue();
    }

    @Test
    void currentUserPromptNotDuplicatedInHistory() {
        ChatSession session = new ChatSession();
        session.addUser("Vorherige Frage");
        session.addAssistant("Vorherige Antwort");

        ContextEnricher enricher = new ContextEnricher(
            refs -> new ResolvedTableResult(List.of(), List.of()),
            table -> "",
            new SampleRowsCollector() {
                @Override
                public List<TableSampleRow> collect(ResolvedTable resolvedTable, int maxRows, int maxColumns) {
                    return List.of();
                }

                @Override
                public String createSampleQueryText(ResolvedTable resolvedTable, int maxRows) {
                    return "";
                }
            },
            new SensitiveDataMasker()
        );

        AtomicReference<LlmRequest> capturedRequest = new AtomicReference<>();
        LlmClient llmClient = (request, listener) -> {
            capturedRequest.set(request);
            listener.onComplete("ok");
            return () -> {
            };
        };

        ChatController controller = new ChatController(
            session,
            new MentionParser(),
            enricher,
            new ContextAssembler(new PromptBudgetEstimator()),
            new ContextAwarePromptComposer(),
            llmClient
        );

        controller.send(
            "system",
            "Neue Frage ohne doppelte History",
            new ChatRequestOptions(1, 1, 1, false, false, 1000, 10),
            new RecordingUiListener()
        );

        LlmRequest request = capturedRequest.get();
        assertThat(request).isNotNull();
        assertThat(request.userPrompt()).contains("Neue Frage ohne doppelte History");
        assertThat(request.history()).hasSize(2);
        assertThat(request.history()).extracting(m -> m.text())
            .containsExactly("Vorherige Frage", "Vorherige Antwort");
    }

    private static final class RecordingUiListener implements ChatUiListener {
        private String beforeSendText;
        private final List<String> partialChunks = new ArrayList<>();
        private String completedText;
        private String promptBlock;
        private final List<String> warnings = new ArrayList<>();
        private String errorMessage;

        @Override
        public void onBeforeSend(String userPrompt) {
            this.beforeSendText = userPrompt;
        }

        @Override
        public void onContextBuilt(ContextBundle contextBundle, String promptBlock) {
            this.promptBlock = promptBlock;
        }

        @Override
        public void onAssistantPartial(String chunk) {
            partialChunks.add(chunk);
        }

        @Override
        public void onAssistantComplete(String finalText) {
            this.completedText = finalText;
        }

        @Override
        public void onWarning(String warning) {
            warnings.add(warning);
        }

        @Override
        public void onError(String message, Throwable error) {
            this.errorMessage = message;
        }
    }
}
