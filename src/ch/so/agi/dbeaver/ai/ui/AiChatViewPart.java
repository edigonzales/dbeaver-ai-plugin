package ch.so.agi.dbeaver.ai.ui;

import ch.so.agi.dbeaver.ai.chat.ChatController;
import ch.so.agi.dbeaver.ai.chat.ChatSession;
import ch.so.agi.dbeaver.ai.chat.ChatUiListener;
import ch.so.agi.dbeaver.ai.config.AiSettings;
import ch.so.agi.dbeaver.ai.config.AiSettingsService;
import ch.so.agi.dbeaver.ai.context.ContextAssembler;
import ch.so.agi.dbeaver.ai.context.ContextEnricher;
import ch.so.agi.dbeaver.ai.context.DBeaverSampleRowsCollector;
import ch.so.agi.dbeaver.ai.context.DBeaverTableDdlExtractor;
import ch.so.agi.dbeaver.ai.context.DBeaverTableReferenceResolver;
import ch.so.agi.dbeaver.ai.context.PromptBudgetEstimator;
import ch.so.agi.dbeaver.ai.context.SensitiveDataMasker;
import ch.so.agi.dbeaver.ai.llm.ContextAwarePromptComposer;
import ch.so.agi.dbeaver.ai.llm.LangChain4jOpenAiClient;
import ch.so.agi.dbeaver.ai.llm.LlmClient;
import ch.so.agi.dbeaver.ai.mention.DBeaverMentionCatalog;
import ch.so.agi.dbeaver.ai.mention.MentionParser;
import ch.so.agi.dbeaver.ai.mention.MentionProposal;
import ch.so.agi.dbeaver.ai.mention.MentionProposalProvider;
import ch.so.agi.dbeaver.ai.mention.MentionTriggerDetector;
import ch.so.agi.dbeaver.ai.model.ContextBundle;
import ch.so.agi.dbeaver.ai.model.TableReference;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;

import java.util.List;

public final class AiChatViewPart extends ViewPart {
    public static final String VIEW_ID = "ch.so.agi.dbeaver.ai.views.chat";

    private final AiSettingsService settingsService = new AiSettingsService();
    private final ChatSession chatSession = new ChatSession();
    private final MentionParser mentionParser = new MentionParser();
    private final MentionTriggerDetector mentionTriggerDetector = new MentionTriggerDetector();
    private final DBeaverMentionCatalog mentionCatalog = new DBeaverMentionCatalog();

    private final ContextEnricher contextEnricher = new ContextEnricher(
        new DBeaverTableReferenceResolver(),
        new DBeaverTableDdlExtractor(),
        new DBeaverSampleRowsCollector(),
        new SensitiveDataMasker()
    );
    private final ContextAssembler contextAssembler = new ContextAssembler(new PromptBudgetEstimator());
    private final ContextAwarePromptComposer promptComposer = new ContextAwarePromptComposer();

    private Text transcriptText;
    private Text inputText;
    private Button sendButton;
    private Button stopButton;
    private Label statusLabel;

    private volatile List<TableReference> mentionCandidates = List.of();
    private MentionProposalProvider mentionProposalProvider;
    private volatile ChatController activeController;

    @Override
    public void createPartControl(Composite parent) {
        Composite root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));
        root.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        transcriptText = new Text(root, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
        transcriptText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite inputRow = new Composite(root, SWT.NONE);
        inputRow.setLayout(new GridLayout(4, false));
        inputRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        inputText = new Text(inputRow, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData inputGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        inputGd.heightHint = 56;
        inputText.setLayoutData(inputGd);

        sendButton = new Button(inputRow, SWT.PUSH);
        sendButton.setText("Send");

        stopButton = new Button(inputRow, SWT.PUSH);
        stopButton.setText("Stop");
        stopButton.setEnabled(false);

        Button refreshMentionsButton = new Button(inputRow, SWT.PUSH);
        refreshMentionsButton.setText("Refresh #");

        statusLabel = new Label(root, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        statusLabel.setText("Ready");

        mentionProposalProvider = new MentionProposalProvider(this::currentMentionCandidates);
        installMentionAutocomplete();

        sendButton.addListener(SWT.Selection, e -> sendPrompt());
        stopButton.addListener(SWT.Selection, e -> stopPrompt());
        refreshMentionsButton.addListener(SWT.Selection, e -> refreshMentions());

        inputText.addListener(SWT.KeyDown, e -> {
            if ((e.stateMask & SWT.MOD1) != 0 && (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)) {
                sendPrompt();
                e.doit = false;
            }
        });

        appendLine("AI Chat bereit. Verwende #datasource.schema.table für Tabellenkontext.");
    }

    @Override
    public void setFocus() {
        if (inputText != null && !inputText.isDisposed()) {
            inputText.setFocus();
        }
    }

    @Override
    public void dispose() {
        stopPrompt();
        super.dispose();
    }

    public void prefillPrompt(String text) {
        if (inputText == null || inputText.isDisposed()) {
            return;
        }
        inputText.setText(text == null ? "" : text);
        inputText.setFocus();
        inputText.setSelection(inputText.getText().length());
    }

    private void sendPrompt() {
        String userPrompt = inputText.getText() == null ? "" : inputText.getText().trim();
        if (userPrompt.isBlank()) {
            return;
        }

        AiSettings settings = settingsService.loadSettings();
        String apiToken = settingsService.loadApiToken();

        if (apiToken.isBlank()) {
            setStatus("Kein API-Token gesetzt. Bitte in den Preferences konfigurieren.");
            return;
        }

        inputText.setText("");

        LlmClient llmClient = new LangChain4jOpenAiClient(
            settings.baseUrl(),
            apiToken,
            settings.model(),
            settings.temperature(),
            settings.timeout()
        );

        ChatController controller = new ChatController(
            chatSession,
            mentionParser,
            contextEnricher,
            contextAssembler,
            promptComposer,
            llmClient
        );
        activeController = controller;

        controller.send(settings.systemPrompt(), userPrompt, settings.toChatRequestOptions(), new ViewChatUiListener());
    }

    private void stopPrompt() {
        ChatController controller = activeController;
        if (controller != null) {
            controller.cancelActiveRequest();
        }
        activeController = null;
        ui(() -> {
            sendButton.setEnabled(true);
            stopButton.setEnabled(false);
            setStatus("Anfrage gestoppt");
        });
    }

    private void refreshMentions() {
        mentionCandidates = mentionCatalog.loadCandidates();
        setStatus("Autocomplete aktualisiert: " + mentionCandidates.size() + " Tabellen referenzierbar");
    }

    private synchronized List<TableReference> currentMentionCandidates() {
        if (mentionCandidates.isEmpty()) {
            mentionCandidates = mentionCatalog.loadCandidates();
        }
        return mentionCandidates;
    }

    private void installMentionAutocomplete() {
        IContentProposalProvider provider = (contents, position) -> {
            if (!mentionTriggerDetector.isInMentionContext(contents, position)) {
                return new IContentProposal[0];
            }

            String prefix = mentionTriggerDetector.currentMentionPrefix(contents, position);
            List<MentionProposal> proposals = mentionProposalProvider.suggest(prefix);
            int proposalLimit = settingsService.loadSettings().mentionProposalLimit();
            int shown = Math.min(proposalLimit, proposals.size());
            setStatus(shown + "/" + proposals.size() + " Treffer");

            return proposals.stream()
                .limit(proposalLimit)
                .map(p -> new ContentProposal(p.insertText(), p.displayText(), null, p.insertText().length()))
                .toArray(IContentProposal[]::new);
        };

        ContentProposalAdapter adapter = new ContentProposalAdapter(
            inputText,
            new TextContentAdapter(),
            provider,
            KeyStroke.getInstance(SWT.MOD1, SWT.SPACE),
            new char[]{'#', '.'}
        );
        adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_INSERT);
        adapter.setPropagateKeys(true);
        adapter.setAutoActivationDelay(80);
    }

    private void appendLine(String text) {
        ui(() -> {
            String existing = transcriptText.getText();
            String next = existing.isBlank() ? text : existing + "\n" + text;
            transcriptText.setText(next);
            transcriptText.setSelection(transcriptText.getText().length());
        });
    }

    private void appendText(String text) {
        ui(() -> {
            transcriptText.append(text);
            transcriptText.setSelection(transcriptText.getText().length());
        });
    }

    private void setStatus(String status) {
        ui(() -> statusLabel.setText(status == null ? "" : status));
    }

    private void ui(Runnable runnable) {
        Display display = getSite() == null ? Display.getDefault() : getSite().getShell().getDisplay();
        if (display.getThread() == Thread.currentThread()) {
            runnable.run();
        } else {
            display.asyncExec(() -> {
                if (transcriptText == null || transcriptText.isDisposed()) {
                    return;
                }
                runnable.run();
            });
        }
    }

    private final class ViewChatUiListener implements ChatUiListener {
        @Override
        public void onBeforeSend(String userPrompt) {
            ui(() -> {
                sendButton.setEnabled(false);
                stopButton.setEnabled(true);
            });
            appendLine("You> " + userPrompt);
            appendText("AI> ");
            setStatus("Anfrage wird verarbeitet...");
        }

        @Override
        public void onContextBuilt(ContextBundle contextBundle, String promptBlock) {
            setStatus("Kontext aufgebaut: " + contextBundle.tableContexts().size() + " Tabelle(n)");
        }

        @Override
        public void onAssistantPartial(String chunk) {
            appendText(chunk);
        }

        @Override
        public void onAssistantComplete(String finalText) {
            appendText("\n");
            ui(() -> {
                sendButton.setEnabled(true);
                stopButton.setEnabled(false);
            });
            setStatus("Antwort vollständig");
            activeController = null;
        }

        @Override
        public void onWarning(String warning) {
            appendLine("[Warnung] " + warning);
        }

        @Override
        public void onError(String message, Throwable error) {
            appendLine("[Fehler] " + message + ": " + (error == null ? "<unknown>" : error.getMessage()));
            ui(() -> {
                sendButton.setEnabled(true);
                stopButton.setEnabled(false);
            });
            setStatus("Fehler bei der Anfrage");
            activeController = null;
        }
    }
}
