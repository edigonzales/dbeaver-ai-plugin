package ch.so.agi.dbeaver.ai.config;

import org.eclipse.swt.SWT;
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
import org.eclipse.jface.preference.PreferencePage;

public final class AiPreferencePageMain extends PreferencePage implements IWorkbenchPreferencePage {
    private final AiSettingsService settingsService = new AiSettingsService();

    private Text baseUrlText;
    private Text modelText;
    private Text systemPromptText;
    private Text apiTokenText;
    private Text sampleRowLimitText;
    private Text maxReferencedTablesText;
    private Text maxColumnsPerSampleText;
    private Text historySizeText;
    private Text maxContextTokensText;
    private Text mentionProposalLimitText;
    private Text temperatureText;
    private Combo llmLogModeCombo;

    private Button includeDdlButton;
    private Button includeSampleRowsButton;
    private Button langchainHttpLoggingButton;
    private Button clearTokenButton;
    private Label tokenStatusLabel;

    @Override
    public void init(IWorkbench workbench) {
        // no-op
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite root = new Composite(parent, SWT.NONE);
        root.setLayout(new GridLayout(2, false));
        root.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

        baseUrlText = createLabeledText(root, "Base URL", SWT.BORDER);
        modelText = createLabeledText(root, "Model", SWT.BORDER);

        Label promptLabel = new Label(root, SWT.NONE);
        promptLabel.setText("System Prompt");
        promptLabel.setLayoutData(new GridData(SWT.LEFT, SWT.TOP, false, false));

        systemPromptText = new Text(root, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        GridData promptGd = new GridData(SWT.FILL, SWT.TOP, true, false);
        promptGd.heightHint = 120;
        promptGd.widthHint = 700;
        systemPromptText.setLayoutData(promptGd);

        apiTokenText = createLabeledText(root, "OpenAI API Token", SWT.BORDER | SWT.PASSWORD);
        apiTokenText.setMessage("Leer lassen, um gespeicherten Token beizubehalten");

        tokenStatusLabel = new Label(root, SWT.NONE);
        tokenStatusLabel.setText("");
        GridData statusGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        statusGd.horizontalSpan = 2;
        tokenStatusLabel.setLayoutData(statusGd);

        clearTokenButton = new Button(root, SWT.CHECK);
        clearTokenButton.setText("Gespeicherten API Token löschen");
        GridData clearGd = new GridData(SWT.FILL, SWT.CENTER, true, false);
        clearGd.horizontalSpan = 2;
        clearTokenButton.setLayoutData(clearGd);

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
        historySizeText = createLabeledText(root, "Chat History Size", SWT.BORDER);
        maxContextTokensText = createLabeledText(root, "Max Context Tokens", SWT.BORDER);
        mentionProposalLimitText = createLabeledText(root, "Autocomplete Proposal Limit", SWT.BORDER);
        temperatureText = createLabeledText(root, "Temperature (0.0 - 2.0)", SWT.BORDER);
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
        updateTokenStatus();

        return root;
    }

    @Override
    public boolean performOk() {
        AiSettings settings = readFromForm();
        settingsService.saveSettings(settings);

        if (clearTokenButton.getSelection()) {
            settingsService.saveApiToken("");
        }

        String enteredToken = apiTokenText.getText() == null ? "" : apiTokenText.getText().trim();
        if (!enteredToken.isEmpty()) {
            settingsService.saveApiToken(enteredToken);
            apiTokenText.setText("");
        }

        updateTokenStatus();
        return true;
    }

    @Override
    protected void performDefaults() {
        loadFromSettings(new AiSettings(
            AiSettings.DEFAULT_BASE_URL,
            AiSettings.DEFAULT_MODEL,
            AiSettings.DEFAULT_SYSTEM_PROMPT,
            5,
            8,
            30,
            AiSettings.DEFAULT_INCLUDE_DDL,
            AiSettings.DEFAULT_INCLUDE_SAMPLE_ROWS,
            12,
            4_000,
            AiSettings.DEFAULT_MENTION_PROPOSAL_LIMIT,
            AiSettings.DEFAULT_LLM_LOG_MODE,
            AiSettings.DEFAULT_LANGCHAIN_HTTP_LOGGING,
            0.0
        ));
        clearTokenButton.setSelection(false);
        apiTokenText.setText("");
        super.performDefaults();
    }

    private void loadFromSettings(AiSettings settings) {
        baseUrlText.setText(settings.baseUrl());
        modelText.setText(settings.model());
        systemPromptText.setText(settings.systemPrompt());

        sampleRowLimitText.setText(Integer.toString(settings.sampleRowLimit()));
        maxReferencedTablesText.setText(Integer.toString(settings.maxReferencedTables()));
        maxColumnsPerSampleText.setText(Integer.toString(settings.maxColumnsPerSample()));
        historySizeText.setText(Integer.toString(settings.historySize()));
        maxContextTokensText.setText(Integer.toString(settings.maxContextTokens()));
        mentionProposalLimitText.setText(Integer.toString(settings.mentionProposalLimit()));
        temperatureText.setText(Double.toString(settings.temperature()));
        llmLogModeCombo.setText(settings.llmLogMode().name());

        includeDdlButton.setSelection(settings.includeDdl());
        includeSampleRowsButton.setSelection(settings.includeSampleRows());
        langchainHttpLoggingButton.setSelection(settings.langchainHttpLogging());
    }

    private AiSettings readFromForm() {
        return new AiSettings(
            safeTrim(baseUrlText.getText(), AiSettings.DEFAULT_BASE_URL),
            safeTrim(modelText.getText(), AiSettings.DEFAULT_MODEL),
            safeTrim(systemPromptText.getText(), AiSettings.DEFAULT_SYSTEM_PROMPT),
            parseIntOrDefault(sampleRowLimitText.getText(), 5),
            parseIntOrDefault(maxReferencedTablesText.getText(), 8),
            parseIntOrDefault(maxColumnsPerSampleText.getText(), 30),
            includeDdlButton.getSelection(),
            includeSampleRowsButton.getSelection(),
            parseIntOrDefault(historySizeText.getText(), 12),
            parseIntOrDefault(maxContextTokensText.getText(), 4_000),
            parseIntOrDefault(mentionProposalLimitText.getText(), AiSettings.DEFAULT_MENTION_PROPOSAL_LIMIT),
            parseLlmLogMode(llmLogModeCombo.getText()),
            langchainHttpLoggingButton.getSelection(),
            parseDoubleOrDefault(temperatureText.getText(), 0.0)
        );
    }

    private void updateTokenStatus() {
        tokenStatusLabel.setText(settingsService.hasApiToken()
            ? "API Token ist im Secret Store gespeichert"
            : "Kein API Token gespeichert");
        tokenStatusLabel.getParent().layout(true, true);
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
}
