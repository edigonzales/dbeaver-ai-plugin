package ch.so.agi.dbeaver.ai.config;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class AiPreferencePageMain extends PreferencePage implements IWorkbenchPreferencePage {
    private final AiSettingsService settingsService = new AiSettingsService();

    private Text systemPromptText;
    private Text sampleRowLimitText;
    private Text maxReferencedTablesText;
    private Text maxColumnsPerSampleText;
    private Text historySizeText;
    private Text maxContextTokensText;
    private Text mentionProposalLimitText;
    private Text mentionCandidateLimitText;
    private Text temperatureText;
    private Text timeoutSecondsText;
    private Combo llmLogModeCombo;
    private Label sampleRowsHintLabel;

    private Button includeDdlButton;
    private Button includeSampleRowsButton;
    private Button langchainHttpLoggingButton;

    private ScrolledComposite endpointRowsScroller;
    private Composite endpointRowsContainer;
    private final List<EndpointRow> endpointRows = new ArrayList<>();
    private Set<String> initialUserEndpointIds = Set.of();

    @Override
    public void init(IWorkbench workbench) {
        // no-op
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(2, false));
        root.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        Label endpointLabel = new Label(root, SWT.NONE);
        endpointLabel.setText("LLM Endpoints");
        endpointLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        Composite endpointArea = new Composite(root, SWT.NONE);
        GridLayout endpointAreaLayout = new GridLayout(1, false);
        endpointAreaLayout.marginWidth = 0;
        endpointAreaLayout.marginHeight = 0;
        endpointAreaLayout.verticalSpacing = 4;
        endpointArea.setLayout(endpointAreaLayout);
        endpointArea.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

        Composite endpointHeader = new Composite(endpointArea, SWT.NONE);
        GridLayout endpointHeaderLayout = new GridLayout(5, false);
        endpointHeaderLayout.marginWidth = 0;
        endpointHeaderLayout.marginHeight = 0;
        endpointHeaderLayout.verticalSpacing = 4;
        endpointHeaderLayout.horizontalSpacing = 8;
        endpointHeader.setLayout(endpointHeaderLayout);
        endpointHeader.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        createHeaderLabel(endpointHeader, "Base URL");
        createHeaderLabel(endpointHeader, "Models (comma-separated)");
        createHeaderLabel(endpointHeader, "API Key");
        createHeaderLabel(endpointHeader, "Key-Status");
        createHeaderLabel(endpointHeader, "");

        endpointRowsScroller = new ScrolledComposite(endpointArea, SWT.V_SCROLL | SWT.BORDER);
        endpointRowsScroller.setExpandHorizontal(true);
        endpointRowsScroller.setExpandVertical(true);
        GridData endpointRowsScrollerGd = new GridData(SWT.FILL, SWT.FILL, true, false);
        endpointRowsScrollerGd.heightHint = 210;
        endpointRowsScroller.setLayoutData(endpointRowsScrollerGd);

        endpointRowsContainer = new Composite(endpointRowsScroller, SWT.NONE);
        GridLayout endpointRowsLayout = new GridLayout(1, false);
        endpointRowsLayout.marginWidth = 0;
        endpointRowsLayout.marginHeight = 0;
        endpointRowsLayout.verticalSpacing = 4;
        endpointRowsContainer.setLayout(endpointRowsLayout);
        endpointRowsScroller.setContent(endpointRowsContainer);

        Composite endpointActions = new Composite(endpointArea, SWT.NONE);
        GridLayout endpointActionsLayout = new GridLayout(1, false);
        endpointActionsLayout.marginWidth = 0;
        endpointActionsLayout.marginHeight = 0;
        endpointActions.setLayout(endpointActionsLayout);
        endpointActions.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        Button addEndpointButton = new Button(endpointActions, SWT.PUSH);
        addEndpointButton.setText("Add Endpoint");
        addEndpointButton.addListener(SWT.Selection, e -> {
            addEmptyUserEndpointRow(UUID.randomUUID().toString());
            relayoutEndpointRows();
        });

        Label endpointHintLabel = new Label(endpointArea, SWT.WRAP);
        endpointHintLabel.setText("Der OpenAI-Endpunkt ist fix. Für API-Keys: Feld leer lassen behält den gespeicherten Key.");
        endpointHintLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        Label promptLabel = new Label(root, SWT.NONE);
        promptLabel.setText("System Prompt");
        promptLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        systemPromptText = new Text(root, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData promptGd = new GridData(SWT.FILL, SWT.TOP, true, false);
        promptGd.heightHint = 120;
        promptGd.widthHint = 700;
        systemPromptText.setLayoutData(promptGd);

        includeDdlButton = new Button(root, SWT.CHECK);
        includeDdlButton.setText("DDL im Kontext mitsenden");
        GridData includeDdlGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        includeDdlGd.horizontalSpan = 2;
        includeDdlButton.setLayoutData(includeDdlGd);

        includeSampleRowsButton = new Button(root, SWT.CHECK);
        includeSampleRowsButton.setText("Sample Rows im Kontext mitsenden");
        GridData includeSampleRowsGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        includeSampleRowsGd.horizontalSpan = 2;
        includeSampleRowsButton.setLayoutData(includeSampleRowsGd);

        sampleRowLimitText = createLabeledText(root, "Sample Row Limit", SWT.BORDER);
        maxReferencedTablesText = createLabeledText(root, "Max Referenced Tables", SWT.BORDER);
        maxColumnsPerSampleText = createLabeledText(root, "Max Columns per Sample", SWT.BORDER);

        sampleRowsHintLabel = new Label(root, SWT.WRAP);
        sampleRowsHintLabel.setText(
            "Hinweis: Sample Rows werden derzeit nicht an das Modell gesendet. Diese Felder bleiben nur aus Kompatibilitaetsgruenden sichtbar."
        );
        GridData sampleRowsHintGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        sampleRowsHintGd.horizontalSpan = 2;
        sampleRowsHintGd.widthHint = 700;
        sampleRowsHintLabel.setLayoutData(sampleRowsHintGd);

        historySizeText = createLabeledText(root, "Chat History Size", SWT.BORDER);
        maxContextTokensText = createLabeledText(root, "Max Context Tokens", SWT.BORDER);
        mentionProposalLimitText = createLabeledText(root, "Autocomplete Proposal Limit", SWT.BORDER);
        mentionCandidateLimitText = createLabeledText(root, "Autocomplete Candidate Scan Limit", SWT.BORDER);
        temperatureText = createLabeledText(root, "Temperature (0.0 - 2.0)", SWT.BORDER);
        timeoutSecondsText = createLabeledText(root, "Timeout (Sekunden, 10-600)", SWT.BORDER);
        llmLogModeCombo = createLabeledCombo(root, "LLM Logging", new String[]{"OFF", "METADATA", "FULL"});

        langchainHttpLoggingButton = new Button(root, SWT.CHECK);
        langchainHttpLoggingButton.setText("LangChain HTTP Logging (Request/Response)");
        GridData langchainLoggingGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        langchainLoggingGd.horizontalSpan = 2;
        langchainHttpLoggingButton.setLayoutData(langchainLoggingGd);

        Label loggingHintLabel = new Label(root, SWT.WRAP);
        loggingHintLabel.setText("Hinweis: LLM Logging = FULL schreibt vollständige Prompt- und Antworttexte ins Error Log.");
        GridData loggingHintGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        loggingHintGd.horizontalSpan = 2;
        loggingHintGd.widthHint = 700;
        loggingHintLabel.setLayoutData(loggingHintGd);

        loadFromSettings(settingsService.loadSettings());
        return root;
    }

    @Override
    public boolean performOk() {
        setErrorMessage(null);
        AiSettings settings = readFromForm();
        if (settings == null) {
            return false;
        }

        Set<String> currentUserIds = new HashSet<>();
        for (LlmEndpointConfig endpoint : settings.endpoints()) {
            currentUserIds.add(endpoint.id());
        }

        settingsService.saveSettings(settings);

        for (EndpointRow row : endpointRows) {
            String enteredToken = row.apiKeyText.getText() == null ? "" : row.apiKeyText.getText().trim();
            if (!enteredToken.isEmpty()) {
                settingsService.saveApiToken(row.endpointId, enteredToken);
                row.apiKeyText.setText("");
            }
        }

        for (String removedId : initialUserEndpointIds) {
            if (!currentUserIds.contains(removedId)) {
                settingsService.deleteApiToken(removedId);
            }
        }

        initialUserEndpointIds = currentUserIds;
        refreshEndpointKeyStatuses();
        return true;
    }

    @Override
    protected void performDefaults() {
        loadFromSettings(new AiSettings(
            List.of(),
            AiSettings.DEFAULT_SYSTEM_PROMPT,
            AiSettings.DEFAULT_SAMPLE_ROW_LIMIT,
            AiSettings.DEFAULT_MAX_REFERENCED_TABLES,
            AiSettings.DEFAULT_MAX_COLUMNS_PER_SAMPLE,
            AiSettings.DEFAULT_INCLUDE_DDL,
            AiSettings.DEFAULT_INCLUDE_SAMPLE_ROWS,
            AiSettings.DEFAULT_HISTORY_SIZE,
            AiSettings.DEFAULT_MAX_CONTEXT_TOKENS,
            AiSettings.DEFAULT_MENTION_PROPOSAL_LIMIT,
            AiSettings.DEFAULT_MENTION_CANDIDATE_LIMIT,
            AiSettings.DEFAULT_LLM_LOG_MODE,
            AiSettings.DEFAULT_LANGCHAIN_HTTP_LOGGING,
            AiSettings.DEFAULT_TEMPERATURE,
            AiSettings.DEFAULT_TIMEOUT_SECONDS
        ));
        super.performDefaults();
    }

    private void loadFromSettings(AiSettings settings) {
        clearEndpointRows();
        addEndpointRow(LlmEndpointConfig.builtin(
            AiSettings.BUILTIN_OPENAI_ENDPOINT_ID,
            AiSettings.BUILTIN_OPENAI_BASE_URL,
            AiSettings.BUILTIN_OPENAI_MODELS
        ));
        for (LlmEndpointConfig endpoint : settings.endpoints()) {
            addEndpointRow(endpoint);
        }

        systemPromptText.setText(settings.systemPrompt());

        sampleRowLimitText.setText(Integer.toString(settings.sampleRowLimit()));
        maxReferencedTablesText.setText(Integer.toString(settings.maxReferencedTables()));
        maxColumnsPerSampleText.setText(Integer.toString(settings.maxColumnsPerSample()));
        historySizeText.setText(Integer.toString(settings.historySize()));
        maxContextTokensText.setText(Integer.toString(settings.maxContextTokens()));
        mentionProposalLimitText.setText(Integer.toString(settings.mentionProposalLimit()));
        mentionCandidateLimitText.setText(Integer.toString(settings.mentionCandidateLimit()));
        temperatureText.setText(Double.toString(settings.temperature()));
        timeoutSecondsText.setText(Integer.toString(settings.timeoutSeconds()));
        llmLogModeCombo.setText(settings.llmLogMode().name());

        includeDdlButton.setSelection(settings.includeDdl());
        includeSampleRowsButton.setSelection(false);
        includeSampleRowsButton.setEnabled(false);
        sampleRowLimitText.setEnabled(false);
        maxColumnsPerSampleText.setEnabled(false);
        langchainHttpLoggingButton.setSelection(settings.langchainHttpLogging());

        Set<String> ids = new HashSet<>();
        for (LlmEndpointConfig endpoint : settings.endpoints()) {
            ids.add(endpoint.id());
        }
        initialUserEndpointIds = ids;
        refreshEndpointKeyStatuses();
        relayoutEndpointRows();
    }

    private AiSettings readFromForm() {
        List<LlmEndpointConfig> userEndpoints = new ArrayList<>();
        Set<String> seenBaseUrls = new HashSet<>();

        for (EndpointRow row : endpointRows) {
            if (row.builtin) {
                continue;
            }
            String baseUrl = safeTrim(row.baseUrlText.getText(), "");
            if (baseUrl.isEmpty()) {
                setErrorMessage("Base URL darf nicht leer sein.");
                return null;
            }

            String dedupe = baseUrl.toLowerCase();
            if (!seenBaseUrls.add(dedupe)) {
                setErrorMessage("Doppelte Base URL ist nicht erlaubt: " + baseUrl);
                return null;
            }

            List<String> models = AiSettingsService.parseModelsCsv(row.modelsText.getText());
            userEndpoints.add(LlmEndpointConfig.user(row.endpointId, baseUrl, models));
        }

        return new AiSettings(
            userEndpoints,
            safeTrim(systemPromptText.getText(), AiSettings.DEFAULT_SYSTEM_PROMPT),
            parseIntOrDefault(sampleRowLimitText.getText(), AiSettings.DEFAULT_SAMPLE_ROW_LIMIT),
            parseIntOrDefault(maxReferencedTablesText.getText(), AiSettings.DEFAULT_MAX_REFERENCED_TABLES),
            parseIntOrDefault(maxColumnsPerSampleText.getText(), AiSettings.DEFAULT_MAX_COLUMNS_PER_SAMPLE),
            includeDdlButton.getSelection(),
            false,
            parseIntOrDefault(historySizeText.getText(), AiSettings.DEFAULT_HISTORY_SIZE),
            parseIntOrDefault(maxContextTokensText.getText(), AiSettings.DEFAULT_MAX_CONTEXT_TOKENS),
            parseIntOrDefault(mentionProposalLimitText.getText(), AiSettings.DEFAULT_MENTION_PROPOSAL_LIMIT),
            parseIntOrDefault(mentionCandidateLimitText.getText(), AiSettings.DEFAULT_MENTION_CANDIDATE_LIMIT),
            parseLlmLogMode(llmLogModeCombo.getText()),
            langchainHttpLoggingButton.getSelection(),
            parseDoubleOrDefault(temperatureText.getText(), AiSettings.DEFAULT_TEMPERATURE),
            parseIntOrDefault(timeoutSecondsText.getText(), AiSettings.DEFAULT_TIMEOUT_SECONDS)
        );
    }

    private void clearEndpointRows() {
        for (EndpointRow row : endpointRows) {
            if (!row.container.isDisposed()) {
                row.container.dispose();
            }
        }
        endpointRows.clear();
    }

    private void addEndpointRow(LlmEndpointConfig endpoint) {
        addEndpointRow(endpoint.id(), endpoint.builtin(), endpoint.baseUrl(), endpoint.models());
    }

    private void addEmptyUserEndpointRow(String endpointId) {
        addEndpointRow(endpointId, false, "", List.of());
    }

    private void addEndpointRow(String endpointId, boolean builtin, String baseUrl, List<String> models) {
        Composite row = new Composite(endpointRowsContainer, SWT.NONE);
        GridLayout rowLayout = new GridLayout(5, false);
        rowLayout.marginWidth = 0;
        rowLayout.marginHeight = 0;
        rowLayout.verticalSpacing = 4;
        rowLayout.horizontalSpacing = 8;
        row.setLayout(rowLayout);
        row.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));

        Text baseUrlText = new Text(row, SWT.BORDER);
        baseUrlText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        baseUrlText.setText(baseUrl == null ? "" : baseUrl);

        Text modelsText = new Text(row, SWT.BORDER);
        modelsText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        modelsText.setText(models == null ? "" : String.join(", ", models));

        Text apiKeyText = new Text(row, SWT.BORDER | SWT.PASSWORD);
        apiKeyText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        apiKeyText.setMessage("Neuen Key setzen...");

        Label keyStatusLabel = new Label(row, SWT.NONE);
        GridData keyStatusGd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        keyStatusGd.widthHint = 130;
        keyStatusLabel.setLayoutData(keyStatusGd);

        Button removeButton = new Button(row, SWT.PUSH);
        removeButton.setText("Remove");
        GridData removeButtonGd = new GridData(SWT.FILL, SWT.CENTER, false, false);
        removeButtonGd.widthHint = 90;
        removeButton.setLayoutData(removeButtonGd);

        EndpointRow endpointRow = new EndpointRow(endpointId, builtin, row, baseUrlText, modelsText, apiKeyText, keyStatusLabel);
        endpointRows.add(endpointRow);

        if (builtin) {
            baseUrlText.setEnabled(false);
            modelsText.setEnabled(false);
            removeButton.setEnabled(false);
            removeButton.setText("Fixed");
        } else {
            removeButton.addListener(SWT.Selection, e -> {
                endpointRows.remove(endpointRow);
                row.dispose();
                relayoutEndpointRows();
            });
        }

        updateKeyStatus(endpointRow);
        relayoutEndpointRows();
    }

    private void refreshEndpointKeyStatuses() {
        for (EndpointRow row : endpointRows) {
            updateKeyStatus(row);
        }
    }

    private void updateKeyStatus(EndpointRow row) {
        row.keyStatusLabel.setText(settingsService.hasApiToken(row.endpointId)
            ? "Key gespeichert"
            : "Kein Key");
        row.keyStatusLabel.getParent().layout(true, true);
    }

    private void relayoutEndpointRows() {
        if (endpointRowsContainer == null || endpointRowsContainer.isDisposed()) {
            return;
        }
        endpointRowsContainer.layout(true, true);
        if (endpointRowsScroller != null && !endpointRowsScroller.isDisposed()) {
            endpointRowsScroller.setMinSize(endpointRowsContainer.computeSize(SWT.DEFAULT, SWT.DEFAULT));
            endpointRowsScroller.layout(true, true);
            Composite parent = endpointRowsScroller.getParent();
            if (parent != null && !parent.isDisposed()) {
                parent.layout(true, true);
            }
        }
    }

    private void createHeaderLabel(Composite parent, String text) {
        Label label = new Label(parent, SWT.NONE);
        label.setText(text);
        label.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    private Text createLabeledText(Composite parent, String label, int style) {
        Label l = new Label(parent, SWT.NONE);
        l.setText(label);

        Text t = new Text(parent, style);
        t.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        return t;
    }

    private Combo createLabeledCombo(Composite parent, String label, String[] values) {
        Label l = new Label(parent, SWT.NONE);
        l.setText(label);

        Combo combo = new Combo(parent, SWT.DROP_DOWN | SWT.READ_ONLY);
        combo.setItems(values);
        combo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
        if (values.length > 0) {
            combo.select(0);
        }
        return combo;
    }

    private int parseIntOrDefault(String text, int fallback) {
        try {
            return Integer.parseInt(text.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private double parseDoubleOrDefault(String text, double fallback) {
        try {
            return Double.parseDouble(text.trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }

    private String safeTrim(String text, String fallback) {
        if (text == null || text.isBlank()) {
            return fallback;
        }
        return text.trim();
    }

    private LlmLogMode parseLlmLogMode(String value) {
        return LlmLogMode.fromPreferenceValue(value);
    }

    private record EndpointRow(
        String endpointId,
        boolean builtin,
        Composite container,
        Text baseUrlText,
        Text modelsText,
        Text apiKeyText,
        Label keyStatusLabel
    ) {
    }
}
