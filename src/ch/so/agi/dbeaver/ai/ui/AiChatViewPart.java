package ch.so.agi.dbeaver.ai.ui;

import ch.so.agi.dbeaver.ai.chat.ChatController;
import ch.so.agi.dbeaver.ai.chat.PromptAugmentation;
import ch.so.agi.dbeaver.ai.chat.ChatSession;
import ch.so.agi.dbeaver.ai.chat.ChatUiListener;
import ch.so.agi.dbeaver.ai.config.AiPreferenceConstants;
import ch.so.agi.dbeaver.ai.config.AiSettings;
import ch.so.agi.dbeaver.ai.config.AiSettingsService;
import ch.so.agi.dbeaver.ai.config.LlmEndpointConfig;
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
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvent;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposal;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ProgressBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.part.ViewPart;

import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class AiChatViewPart extends ViewPart implements ISaveablePart {
    private static final Log LOG = Log.getLog(AiChatViewPart.class);
    public static final String VIEW_ID = "ch.so.agi.dbeaver.ai.views.chat";
    private static final int[] DEFAULT_SPLIT_WEIGHTS = new int[]{70, 30};
    private static final String UNTITLED_PROMPT = "Untitled";
    private static final String SEND_BUTTON_TEXT = "Send";
    private static final String SEND_BUTTON_BUSY_TEXT = "Working...";
    private static final String SEND_BUTTON_TOOLTIP = "Send prompt";
    private static final String SEND_BUTTON_BUSY_TOOLTIP = "LLM arbeitet...";
    private static final String INITIAL_TRANSCRIPT_TEXT =
        "AI Chat bereit. Verwende #datasource.schema.table fuer Tabellenkontext oder @sql fuer die aktive Query.";
    private static final String ASSISTANT_PROMPT_PREFIX = "AI> ";
    private static final String ASSISTANT_PLACEHOLDER = "...";

    private final AiSettingsService settingsService = new AiSettingsService();
    private ChatSession chatSession = new ChatSession();
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
    private final PromptFileService promptFileService = new PromptFileService();
    private final MessageLogService messageLogService = new MessageLogService();
    private final PromptDocumentState promptDocumentState = new PromptDocumentState();
    private final SqlPromptInjectionResolver sqlPromptInjectionResolver = new SqlPromptInjectionResolver();

    private SashForm verticalSashForm;
    private Text transcriptText;
    private Text inputText;
    private Combo endpointCombo;
    private Combo modelCombo;
    private Button sendButton;
    private Button stopButton;
    private ProgressBar busyProgressBar;
    private Label statusLabel;
    private Action openPromptAction;
    private Action savePromptAction;
    private Action savePromptAsAction;

    private volatile List<TableReference> mentionCandidates = List.of();
    private volatile boolean mentionCandidatesDirty = true;
    private boolean dirty;
    private final Set<DBPDataSourceRegistry> registeredDataSourceRegistries = new HashSet<>();
    private final Object mentionListenerLock = new Object();
    private DBPPreferenceStore preferenceStore;
    private boolean awaitingFirstAssistantChunk;
    private int assistantContentOffset = -1;
    private int assistantContentLength;
    private boolean busyState;
    private List<LlmEndpointConfig> selectableEndpoints = List.of();

    private final DBPEventListener mentionDataSourceListener = event -> {
        if (event == null) {
            return;
        }
        DBPEvent.Action action = event.getAction();
        if (action == DBPEvent.Action.AFTER_CONNECT
            || action == DBPEvent.Action.OBJECT_ADD
            || action == DBPEvent.Action.OBJECT_REMOVE
            || action == DBPEvent.Action.OBJECT_UPDATE) {
            markMentionCandidatesDirty();
        }
    };

    private final DBPPreferenceListener mentionPreferenceListener = event -> {
        if (event != null && AiPreferenceConstants.PREF_MENTION_CANDIDATE_LIMIT.equals(event.getProperty())) {
            markMentionCandidatesDirty();
        }
    };

    private MentionProposalProvider mentionProposalProvider;
    private volatile ChatController activeController;

    @Override
    public void createPartControl(Composite parent) {
        Composite root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(1, false));
        root.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        verticalSashForm = new SashForm(root, SWT.VERTICAL);
        verticalSashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        transcriptText = new Text(verticalSashForm, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
        transcriptText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Composite promptArea = new Composite(verticalSashForm, SWT.NONE);
        promptArea.setLayout(new GridLayout(1, false));
        promptArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        inputText = new Text(promptArea, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData inputGd = new GridData(SWT.FILL, SWT.FILL, true, true);
        inputGd.heightHint = 120;
        inputText.setLayoutData(inputGd);
        inputText.addModifyListener(e -> refreshDocumentState());

        Composite endpointRow = new Composite(promptArea, SWT.NONE);
        endpointRow.setLayout(new GridLayout(4, false));
        endpointRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label endpointLabel = new Label(endpointRow, SWT.NONE);
        endpointLabel.setText("Endpoint");
        endpointCombo = new Combo(endpointRow, SWT.DROP_DOWN | SWT.READ_ONLY);
        endpointCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label modelLabel = new Label(endpointRow, SWT.NONE);
        modelLabel.setText("Model");
        modelCombo = new Combo(endpointRow, SWT.DROP_DOWN | SWT.READ_ONLY);
        modelCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Composite buttonRow = new Composite(promptArea, SWT.NONE);
        buttonRow.setLayout(new GridLayout(4, false));
        buttonRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        sendButton = new Button(buttonRow, SWT.PUSH);
        sendButton.setText(SEND_BUTTON_TEXT);
        sendButton.setToolTipText(SEND_BUTTON_TOOLTIP);

        stopButton = new Button(buttonRow, SWT.PUSH);
        stopButton.setText("Stop");
        stopButton.setEnabled(false);

        Button clearContextButton = new Button(buttonRow, SWT.PUSH);
        clearContextButton.setText("Clear Context");

        Button refreshMentionsButton = new Button(buttonRow, SWT.PUSH);
        refreshMentionsButton.setText("Refresh #");

        Composite statusRow = new Composite(root, SWT.NONE);
        GridLayout statusRowLayout = new GridLayout(2, false);
        statusRowLayout.marginWidth = 0;
        statusRowLayout.marginHeight = 0;
        statusRow.setLayout(statusRowLayout);
        statusRow.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        busyProgressBar = new ProgressBar(statusRow, SWT.INDETERMINATE);
        GridData busyProgressBarLayout = new GridData(SWT.FILL, SWT.CENTER, false, false);
        busyProgressBarLayout.widthHint = 140;
        busyProgressBar.setLayoutData(busyProgressBarLayout);

        statusLabel = new Label(statusRow, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        statusLabel.setText("Ready");
        setBusyIndicatorVisible(false);

        mentionProposalProvider = new MentionProposalProvider(this::currentMentionCandidates);
        installMentionRefreshHooks();
        installMentionAutocomplete();
        createViewActions();
        contributeViewActions();
        restoreSplitWeights();
        verticalSashForm.addListener(SWT.Selection, e -> persistSplitWeights());

        sendButton.addListener(SWT.Selection, e -> sendPrompt());
        stopButton.addListener(SWT.Selection, e -> stopPrompt());
        clearContextButton.addListener(SWT.Selection, e -> clearContext());
        refreshMentionsButton.addListener(SWT.Selection, e -> refreshMentions());
        endpointCombo.addListener(SWT.Selection, e -> onEndpointSelectionChanged());
        modelCombo.addListener(SWT.Selection, e -> onModelSelectionChanged());

        inputText.addListener(SWT.KeyDown, e -> {
            if ((e.stateMask & SWT.MOD1) != 0 && (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)) {
                sendPrompt();
                e.doit = false;
            }
        });

        promptDocumentState.resetToUntitled("");
        refreshDocumentState();
        reloadEndpointSelectionControls();
        resetTranscriptToInitialState();
    }

    @Override
    public void setFocus() {
        registerDataSourceListeners();
        refreshMentionsIfDirty();
        reloadEndpointSelectionControls();
        if (inputText != null && !inputText.isDisposed()) {
            inputText.setFocus();
        }
    }

    @Override
    public void dispose() {
        stopPrompt();
        persistSplitWeights();
        disposeMentionRefreshHooks();
        super.dispose();
    }

    public void prefillPrompt(String text) {
        replacePromptWithUntitledDraft(text);
    }

    public boolean replacePromptWithUntitledDraft(String text) {
        if (inputText == null || inputText.isDisposed()) {
            return false;
        }
        if (!confirmProceedWithDirtyPrompt("der Prompt ersetzt wird")) {
            return false;
        }
        String nextText = text == null ? "" : text;
        promptDocumentState.resetToUntitled(nextText);
        if (!nextText.equals(inputText.getText())) {
            inputText.setText(nextText);
        }
        refreshDocumentState();
        inputText.setFocus();
        inputText.setSelection(inputText.getText().length());
        return true;
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        savePrompt(false);
    }

    @Override
    public void doSaveAs() {
        savePrompt(true);
    }

    @Override
    public boolean isDirty() {
        return dirty;
    }

    @Override
    public boolean isSaveAsAllowed() {
        return true;
    }

    @Override
    public boolean isSaveOnCloseNeeded() {
        return isDirty();
    }

    private void createViewActions() {
        ISharedImages sharedImages = getSite().getWorkbenchWindow().getWorkbench().getSharedImages();

        openPromptAction = new Action("Open...") {
            @Override
            public void run() {
                openPrompt();
            }
        };
        openPromptAction.setToolTipText("Open prompt from file");
        openPromptAction.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER));

        savePromptAction = new Action("Save") {
            @Override
            public void run() {
                doSave(null);
            }
        };
        savePromptAction.setToolTipText("Save prompt");
        savePromptAction.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_ETOOL_SAVE_EDIT));

        savePromptAsAction = new Action("Save As...") {
            @Override
            public void run() {
                doSaveAs();
            }
        };
        savePromptAsAction.setToolTipText("Save prompt as");
        savePromptAsAction.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_ETOOL_SAVEAS_EDIT));
    }

    private void contributeViewActions() {
        IToolBarManager toolBarManager = getViewSite().getActionBars().getToolBarManager();
        IMenuManager menuManager = getViewSite().getActionBars().getMenuManager();

        toolBarManager.add(openPromptAction);
        toolBarManager.add(savePromptAction);
        toolBarManager.add(savePromptAsAction);

        menuManager.add(openPromptAction);
        menuManager.add(savePromptAction);
        menuManager.add(savePromptAsAction);

        getViewSite().getActionBars().updateActionBars();
    }

    private void openPrompt() {
        if (!confirmProceedWithDirtyPrompt("ein Prompt geladen wird")) {
            return;
        }
        Path targetPath = choosePromptPath(false);
        if (targetPath == null) {
            return;
        }

        try {
            String content = promptFileService.load(targetPath);
            promptDocumentState.markOpened(targetPath, content);
            if (!content.equals(inputText.getText())) {
                inputText.setText(content);
            }
            rememberLastPromptPath(targetPath);
            refreshDocumentState();
            setStatus("Prompt geladen: " + targetPath.getFileName());
            inputText.setFocus();
            inputText.setSelection(inputText.getText().length());
        } catch (IOException ex) {
            LOG.error("Failed to open prompt file", ex);
            MessageDialog.openError(shell(), "Prompt öffnen", "Datei konnte nicht geladen werden:\n" + ex.getMessage());
            setStatus("Prompt konnte nicht geladen werden");
        }
    }

    private boolean savePrompt(boolean forceSaveAs) {
        if (inputText == null || inputText.isDisposed()) {
            return false;
        }
        if (!forceSaveAs && !isDirty()) {
            return true;
        }
        Path targetPath = forceSaveAs ? choosePromptPath(true) : currentPromptPath();
        if (targetPath == null && !forceSaveAs) {
            return savePrompt(true);
        }
        if (targetPath == null) {
            return false;
        }

        String content = currentPromptText();
        try {
            promptFileService.saveDraft(targetPath, content);
            promptDocumentState.markSavedDraft(targetPath, content);
            rememberLastPromptPath(targetPath);
            refreshDocumentState();
            setStatus("Prompt gespeichert: " + targetPath.getFileName());
            return true;
        } catch (IOException ex) {
            LOG.error("Failed to save prompt file", ex);
            MessageDialog.openError(shell(), "Prompt speichern", "Datei konnte nicht gespeichert werden:\n" + ex.getMessage());
            setStatus("Prompt konnte nicht gespeichert werden");
            return false;
        }
    }

    private Path choosePromptPath(boolean saveDialog) {
        FileDialog dialog = new FileDialog(shell(), saveDialog ? SWT.SAVE : SWT.OPEN);
        dialog.setText(saveDialog ? "Save Prompt As..." : "Open Prompt");
        dialog.setFilterExtensions(new String[]{"*.txt", "*.*"});
        if (saveDialog) {
            dialog.setOverwrite(true);
        }

        Path suggestion = initialPromptDialogPath(saveDialog);
        if (suggestion != null) {
            Path parent = suggestion.getParent();
            if (parent != null) {
                dialog.setFilterPath(parent.toString());
            }
            Path fileName = suggestion.getFileName();
            if (fileName != null) {
                dialog.setFileName(fileName.toString());
            }
        } else if (saveDialog) {
            dialog.setFileName("prompt.txt");
        }

        String selected = dialog.open();
        if (selected == null || selected.isBlank()) {
            return null;
        }
        try {
            return Path.of(selected).toAbsolutePath().normalize();
        } catch (InvalidPathException ex) {
            MessageDialog.openError(shell(), saveDialog ? "Prompt speichern" : "Prompt öffnen", "Ungültiger Dateipfad:\n" + selected);
            return null;
        }
    }

    private Path initialPromptDialogPath(boolean saveDialog) {
        Path currentPath = currentPromptPath();
        if (currentPath != null) {
            return currentPath;
        }

        String lastPath = readPreference(AiPreferenceConstants.PREF_LAST_PROMPT_PATH);
        if (lastPath != null && !lastPath.isBlank()) {
            try {
                return Path.of(lastPath).toAbsolutePath().normalize();
            } catch (InvalidPathException ex) {
                LOG.debug("Ignoring invalid last prompt path", ex);
            }
        }
        return saveDialog ? Path.of("prompt.txt") : null;
    }

    private Path currentPromptPath() {
        return promptDocumentState.boundPath();
    }

    private boolean confirmProceedWithDirtyPrompt(String actionDescription) {
        if (!isDirty()) {
            return true;
        }

        MessageDialog dialog = new MessageDialog(
            shell(),
            "Ungespeicherter Prompt",
            null,
            "Der aktuelle Prompt enthält ungespeicherte Änderungen. Soll er gespeichert werden, bevor " + actionDescription + "?",
            MessageDialog.QUESTION,
            new String[]{"Save", "Verwerfen", "Abbrechen"},
            0
        );
        int choice = dialog.open();
        if (choice == 0) {
            return savePrompt(false);
        }
        return choice == 1;
    }

    private void refreshDocumentState() {
        boolean nextDirty = promptDocumentState.isDirty(currentPromptText());
        if (dirty != nextDirty) {
            dirty = nextDirty;
            firePropertyChange(PROP_DIRTY);
        }
        updateDocumentPresentation();
    }

    private void updateDocumentPresentation() {
        String label = currentPromptPath() == null ? UNTITLED_PROMPT : currentPromptPath().getFileName().toString();
        if (dirty) {
            label = label + " *";
        }
        setContentDescription(label);
        updateActionEnablement();
    }

    private void updateActionEnablement() {
        if (savePromptAction != null) {
            savePromptAction.setEnabled(isDirty());
        }
        if (savePromptAsAction != null) {
            savePromptAsAction.setEnabled(true);
        }
        if (openPromptAction != null) {
            openPromptAction.setEnabled(true);
        }
        if (getViewSite() != null) {
            getViewSite().getActionBars().updateActionBars();
        }
    }

    private void rememberLastPromptPath(Path path) {
        writePreference(AiPreferenceConstants.PREF_LAST_PROMPT_PATH, path.toString());
    }

    private void restoreSplitWeights() {
        if (verticalSashForm == null || verticalSashForm.isDisposed()) {
            return;
        }
        verticalSashForm.setWeights(parseSplitWeights(readPreference(AiPreferenceConstants.PREF_CHAT_SPLIT_WEIGHTS)));
    }

    private void persistSplitWeights() {
        if (verticalSashForm == null || verticalSashForm.isDisposed()) {
            return;
        }
        int[] weights = verticalSashForm.getWeights();
        if (weights.length != 2 || weights[0] <= 0 || weights[1] <= 0) {
            return;
        }
        writePreference(AiPreferenceConstants.PREF_CHAT_SPLIT_WEIGHTS, weights[0] + "," + weights[1]);
    }

    private int[] parseSplitWeights(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_SPLIT_WEIGHTS.clone();
        }
        String[] parts = value.split(",");
        if (parts.length != 2) {
            return DEFAULT_SPLIT_WEIGHTS.clone();
        }
        try {
            int first = Integer.parseInt(parts[0].trim());
            int second = Integer.parseInt(parts[1].trim());
            if (first <= 0 || second <= 0) {
                return DEFAULT_SPLIT_WEIGHTS.clone();
            }
            return new int[]{first, second};
        } catch (NumberFormatException ex) {
            return DEFAULT_SPLIT_WEIGHTS.clone();
        }
    }

    private String readPreference(String key) {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        return store == null ? "" : store.getString(key);
    }

    private void writePreference(String key, String value) {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        if (store == null) {
            return;
        }
        store.setValue(key, value == null ? "" : value);
        if (store.needsSaving()) {
            try {
                store.save();
            } catch (IOException ex) {
                LOG.warn("Failed to save preference " + key, ex);
            }
        }
    }

    private void reloadEndpointSelectionControls() {
        if (endpointCombo == null || endpointCombo.isDisposed() || modelCombo == null || modelCombo.isDisposed()) {
            return;
        }

        AiSettings settings = settingsService.loadSettings();
        selectableEndpoints = settings.effectiveEndpoints();
        endpointCombo.removeAll();
        for (LlmEndpointConfig endpoint : selectableEndpoints) {
            endpointCombo.add(endpoint.builtin() ? "OpenAI (builtin)" : endpoint.baseUrl());
        }

        AiSettings.EndpointSelection selection = settings.resolveSelection(
            readPreference(AiPreferenceConstants.PREF_CHAT_SELECTED_ENDPOINT_ID),
            readPreference(AiPreferenceConstants.PREF_CHAT_SELECTED_MODEL)
        );

        int endpointIndex = indexOfEndpointById(selection.endpoint().id());
        if (endpointIndex < 0 && !selectableEndpoints.isEmpty()) {
            endpointIndex = 0;
        }
        if (endpointIndex >= 0 && endpointCombo.getItemCount() > endpointIndex) {
            endpointCombo.select(endpointIndex);
        }

        updateModelComboForSelectedEndpoint(selection.modelName());
        persistCurrentSelection();
        updateSendButtonState();
    }

    private int indexOfEndpointById(String endpointId) {
        for (int i = 0; i < selectableEndpoints.size(); i++) {
            if (selectableEndpoints.get(i).id().equals(endpointId)) {
                return i;
            }
        }
        return -1;
    }

    private void updateModelComboForSelectedEndpoint(String preferredModel) {
        modelCombo.removeAll();
        LlmEndpointConfig endpoint = selectedEndpoint();
        if (endpoint == null) {
            return;
        }
        for (String model : endpoint.models()) {
            modelCombo.add(model);
        }
        if (modelCombo.getItemCount() == 0) {
            return;
        }

        int preferredIndex = preferredModel == null ? -1 : modelCombo.indexOf(preferredModel);
        modelCombo.select(preferredIndex >= 0 ? preferredIndex : 0);
    }

    private void onEndpointSelectionChanged() {
        updateModelComboForSelectedEndpoint(readPreference(AiPreferenceConstants.PREF_CHAT_SELECTED_MODEL));
        persistCurrentSelection();
        updateSendButtonState();
    }

    private void onModelSelectionChanged() {
        persistCurrentSelection();
        updateSendButtonState();
    }

    private void persistCurrentSelection() {
        writePreference(AiPreferenceConstants.PREF_CHAT_SELECTED_ENDPOINT_ID, selectedEndpointId());
        writePreference(AiPreferenceConstants.PREF_CHAT_SELECTED_MODEL, selectedModelName());
    }

    private LlmEndpointConfig selectedEndpoint() {
        int index = endpointCombo == null || endpointCombo.isDisposed() ? -1 : endpointCombo.getSelectionIndex();
        if (index < 0 || index >= selectableEndpoints.size()) {
            return null;
        }
        return selectableEndpoints.get(index);
    }

    private String selectedEndpointId() {
        LlmEndpointConfig endpoint = selectedEndpoint();
        return endpoint == null ? "" : endpoint.id();
    }

    private String selectedModelName() {
        if (modelCombo == null || modelCombo.isDisposed()) {
            return "";
        }
        int index = modelCombo.getSelectionIndex();
        if (index < 0 || index >= modelCombo.getItemCount()) {
            return "";
        }
        return modelCombo.getItem(index);
    }

    private void updateSendButtonState() {
        if (sendButton == null || sendButton.isDisposed()) {
            return;
        }
        boolean selectionHasModel = !selectedModelName().isEmpty();
        boolean selectionHasToken = !selectedEndpointId().isEmpty() && settingsService.hasApiToken(selectedEndpointId());
        sendButton.setEnabled(!busyState && selectionHasModel && selectionHasToken);
    }

    private String currentPromptText() {
        if (inputText == null || inputText.isDisposed()) {
            return "";
        }
        String text = inputText.getText();
        return text == null ? "" : text;
    }

    private void sendPrompt() {
        String userPrompt = inputText.getText() == null ? "" : inputText.getText().trim();
        if (userPrompt.isBlank()) {
            return;
        }

        AiSettings settings = settingsService.loadSettings();
        AiSettings.EndpointSelection selection = settings.resolveSelection(selectedEndpointId(), selectedModelName());
        if (!selection.isSendable()) {
            setStatus("Für den gewählten Endpoint ist kein Modell konfiguriert.");
            updateSendButtonState();
            return;
        }

        String apiToken = settingsService.loadApiToken(selection.endpoint().id());
        if (apiToken.isBlank()) {
            setStatus("Kein API-Token für Endpoint gesetzt: " + selection.endpoint().baseUrl());
            updateSendButtonState();
            return;
        }

        PromptAugmentation promptAugmentation = sqlPromptInjectionResolver.resolve(userPrompt);
        appendSentMessageToLog(userPrompt);
        promptDocumentState.resetToUntitled("");
        if (!inputText.getText().isEmpty()) {
            inputText.setText("");
        }
        refreshDocumentState();

        LlmClient llmClient = new LangChain4jOpenAiClient(
            selection.endpoint().baseUrl(),
            apiToken,
            selection.modelName(),
            settings.temperature(),
            settings.timeout(),
            settings.llmLogMode(),
            settings.langchainHttpLogging()
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

        controller.send(settings.systemPrompt(), promptAugmentation, settings.toChatRequestOptions(), new ViewChatUiListener(controller));
    }

    private void appendSentMessageToLog(String userPrompt) {
        try {
            messageLogService.appendUserMessage(userPrompt);
        } catch (IOException ex) {
            LOG.warn("Failed to append sent prompt to message log", ex);
            appendLine("[Warnung] Nachricht konnte nicht ins Log geschrieben werden: "
                + messageLogService.messageLogPath() + " (" + ex.getMessage() + ")");
        }
    }

    private void stopPrompt() {
        ChatController controller = activeController;
        if (controller != null) {
            controller.cancelActiveRequest();
        }
        activeController = null;
        clearAssistantPlaceholder();
        setBusy(false, "Anfrage gestoppt");
    }

    private void clearContext() {
        stopPrompt();
        chatSession.clear();
        chatSession = new ChatSession();
        resetTranscriptToInitialState();
        setStatus("Kontext gelöscht");
    }

    private void refreshMentions() {
        reloadMentionCandidates(true);
    }

    private synchronized List<TableReference> currentMentionCandidates() {
        reloadMentionCandidates(false);
        return mentionCandidates;
    }

    private void refreshMentionsIfDirty() {
        if (mentionCandidatesDirty) {
            reloadMentionCandidates(false);
        }
    }

    private synchronized void reloadMentionCandidates(boolean force) {
        if (!force && !mentionCandidatesDirty) {
            return;
        }
        registerDataSourceListeners();
        int mentionCandidateLimit = settingsService.loadSettings().mentionCandidateLimit();
        mentionCandidates = mentionCatalog.loadCandidates(mentionCandidateLimit);
        mentionCandidatesDirty = false;
        setStatus("Autocomplete aktualisiert: " + mentionCandidates.size() + " Tabellen referenzierbar");
    }

    private void installMentionRefreshHooks() {
        registerDataSourceListeners();
        preferenceStore = DBWorkbench.getPlatform().getPreferenceStore();
        if (preferenceStore != null) {
            preferenceStore.addPropertyChangeListener(mentionPreferenceListener);
        }
    }

    private void disposeMentionRefreshHooks() {
        if (preferenceStore != null) {
            preferenceStore.removePropertyChangeListener(mentionPreferenceListener);
            preferenceStore = null;
        }
        synchronized (mentionListenerLock) {
            for (DBPDataSourceRegistry registry : registeredDataSourceRegistries) {
                try {
                    registry.removeDataSourceListener(mentionDataSourceListener);
                } catch (Exception ex) {
                    LOG.debug("Failed to remove datasource listener for mention refresh", ex);
                }
            }
            registeredDataSourceRegistries.clear();
        }
    }

    private void registerDataSourceListeners() {
        DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
        if (workspace == null) {
            return;
        }
        boolean addedRegistry = false;
        synchronized (mentionListenerLock) {
            for (DBPProject project : workspace.getProjects()) {
                if (project == null) {
                    continue;
                }
                DBPDataSourceRegistry registry = project.getDataSourceRegistry();
                if (registry == null || registeredDataSourceRegistries.contains(registry)) {
                    continue;
                }
                try {
                    registry.addDataSourceListener(mentionDataSourceListener);
                    registeredDataSourceRegistries.add(registry);
                    addedRegistry = true;
                } catch (Exception ex) {
                    LOG.debug("Failed to register datasource listener for mention refresh", ex);
                }
            }
        }
        if (addedRegistry) {
            markMentionCandidatesDirty();
        }
    }

    private void markMentionCandidatesDirty() {
        mentionCandidatesDirty = true;
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

    private void appendLine(ChatController controller, String text) {
        ui(() -> {
            if (!isActiveController(controller)) {
                return;
            }
            String existing = transcriptText.getText();
            String next = existing.isBlank() ? text : existing + "\n" + text;
            transcriptText.setText(next);
            transcriptText.setSelection(transcriptText.getText().length());
        });
    }

    private void setBusy(boolean busy, String status) {
        ui(() -> {
            busyState = busy;
            updateSendButtonState();
            sendButton.setText(busy ? SEND_BUTTON_BUSY_TEXT : SEND_BUTTON_TEXT);
            sendButton.setToolTipText(busy ? SEND_BUTTON_BUSY_TOOLTIP : SEND_BUTTON_TOOLTIP);
            stopButton.setEnabled(busy);
            setBusyIndicatorVisible(busy);
            statusLabel.setText(status == null ? "" : status);
            sendButton.getParent().layout(true, true);
        });
    }

    private void setBusy(ChatController controller, boolean busy, String status) {
        ui(() -> {
            if (!isActiveController(controller)) {
                return;
            }
            busyState = busy;
            updateSendButtonState();
            sendButton.setText(busy ? SEND_BUTTON_BUSY_TEXT : SEND_BUTTON_TEXT);
            sendButton.setToolTipText(busy ? SEND_BUTTON_BUSY_TOOLTIP : SEND_BUTTON_TOOLTIP);
            stopButton.setEnabled(busy);
            setBusyIndicatorVisible(busy);
            statusLabel.setText(status == null ? "" : status);
            sendButton.getParent().layout(true, true);
        });
    }

    private void setBusyIndicatorVisible(boolean visible) {
        if (busyProgressBar == null || busyProgressBar.isDisposed()) {
            return;
        }
        GridData layoutData = (GridData) busyProgressBar.getLayoutData();
        layoutData.exclude = !visible;
        busyProgressBar.setVisible(visible);
        busyProgressBar.getParent().layout(true, true);
    }

    private void setStatus(String status) {
        ui(() -> statusLabel.setText(status == null ? "" : status));
    }

    private void setStatus(ChatController controller, String status) {
        ui(() -> {
            if (!isActiveController(controller)) {
                return;
            }
            statusLabel.setText(status == null ? "" : status);
        });
    }

    private void appendPromptExchange(String userPrompt) {
        ui(() -> {
            String existing = transcriptText.getText();
            StringBuilder next = new StringBuilder(existing);
            if (!existing.isBlank()) {
                next.append('\n');
            }
            next.append("You> ").append(userPrompt);
            next.append('\n');
            next.append(ASSISTANT_PROMPT_PREFIX).append(ASSISTANT_PLACEHOLDER);
            transcriptText.setText(next.toString());
            assistantContentOffset = next.length() - ASSISTANT_PLACEHOLDER.length();
            assistantContentLength = ASSISTANT_PLACEHOLDER.length();
            awaitingFirstAssistantChunk = true;
            transcriptText.setSelection(transcriptText.getText().length());
        });
    }

    private void appendAssistantChunk(ChatController controller, String chunk) {
        if (chunk == null || chunk.isEmpty()) {
            return;
        }
        ui(() -> {
            if (!isActiveController(controller)) {
                return;
            }
            if (!hasActiveAssistantInsertion()) {
                transcriptText.append(chunk);
                transcriptText.setSelection(transcriptText.getText().length());
                return;
            }

            String existing = transcriptText.getText();
            if (awaitingFirstAssistantChunk) {
                replaceAssistantContent(existing, chunk);
                awaitingFirstAssistantChunk = false;
                assistantContentLength = chunk.length();
            } else {
                int insertionOffset = assistantContentOffset + assistantContentLength;
                if (insertionOffset < 0 || insertionOffset > existing.length()) {
                    transcriptText.append(chunk);
                    transcriptText.setSelection(transcriptText.getText().length());
                    resetAssistantInsertion();
                    return;
                }
                String next = existing.substring(0, insertionOffset) + chunk + existing.substring(insertionOffset);
                transcriptText.setText(next);
                assistantContentLength += chunk.length();
            }
            transcriptText.setSelection(transcriptText.getText().length());
        });
    }

    private void finalizeAssistantResponse(ChatController controller, String finalText) {
        ui(() -> {
            if (!isActiveController(controller)) {
                return;
            }
            if (awaitingFirstAssistantChunk && hasActiveAssistantInsertion()) {
                replaceAssistantContent(transcriptText.getText(), finalText == null ? "" : finalText);
                awaitingFirstAssistantChunk = false;
                assistantContentLength = finalText == null ? 0 : finalText.length();
            }
            transcriptText.append("\n");
            transcriptText.setSelection(transcriptText.getText().length());
            resetAssistantInsertion();
        });
    }

    private void clearAssistantPlaceholder() {
        ui(() -> {
            if (awaitingFirstAssistantChunk && hasActiveAssistantInsertion()) {
                replaceAssistantContent(transcriptText.getText(), "");
            }
            transcriptText.setSelection(transcriptText.getText().length());
            resetAssistantInsertion();
        });
    }

    private boolean hasActiveAssistantInsertion() {
        return assistantContentOffset >= 0;
    }

    private boolean isActiveController(ChatController controller) {
        return controller != null && controller == activeController;
    }

    private void resetTranscriptToInitialState() {
        ui(() -> {
            transcriptText.setText(INITIAL_TRANSCRIPT_TEXT);
            transcriptText.setSelection(transcriptText.getText().length());
            resetAssistantInsertion();
        });
    }

    private void replaceAssistantContent(String existing, String replacement) {
        if (!hasActiveAssistantInsertion()) {
            return;
        }
        int contentEndOffset = assistantContentOffset + assistantContentLength;
        if (assistantContentOffset < 0 || contentEndOffset < assistantContentOffset || contentEndOffset > existing.length()) {
            resetAssistantInsertion();
            return;
        }
        String next = existing.substring(0, assistantContentOffset) + replacement + existing.substring(contentEndOffset);
        transcriptText.setText(next);
    }

    private void resetAssistantInsertion() {
        awaitingFirstAssistantChunk = false;
        assistantContentOffset = -1;
        assistantContentLength = 0;
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

    private Shell shell() {
        return getSite() == null ? Display.getDefault().getActiveShell() : getSite().getShell();
    }

    private final class ViewChatUiListener implements ChatUiListener {
        private final ChatController controller;

        private ViewChatUiListener(ChatController controller) {
            this.controller = controller;
        }

        @Override
        public void onBeforeSend(String userPrompt) {
            appendPromptExchange(userPrompt);
            setBusy(controller, true, "LLM arbeitet...");
        }

        @Override
        public void onContextBuilt(ContextBundle contextBundle, String promptBlock) {
            String status = "Kontext aufgebaut: " + contextBundle.tableContexts().size() + " Tabelle(n)";
            if (contextBundle.truncated()) {
                status += " (gekürzt)";
            }
            setStatus(controller, status);
        }

        @Override
        public void onAssistantPartial(String chunk) {
            appendAssistantChunk(controller, chunk);
        }

        @Override
        public void onAssistantComplete(String finalText) {
            ui(() -> {
                if (!isActiveController(controller)) {
                    return;
                }
                finalizeAssistantResponse(controller, finalText);
                activeController = null;
                setBusy(false, "Antwort vollständig");
            });
        }

        @Override
        public void onWarning(String warning) {
            appendLine(controller, "[Warnung] " + warning);
        }

        @Override
        public void onError(String message, Throwable error) {
            ui(() -> {
                if (!isActiveController(controller)) {
                    return;
                }
                clearAssistantPlaceholder();
                appendLine("[Fehler] " + message + ": " + (error == null ? "<unknown>" : error.getMessage()));
                activeController = null;
                setBusy(false, "Fehler bei der Anfrage");
            });
        }
    }
}
