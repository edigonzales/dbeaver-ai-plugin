package ch.so.agi.dbeaver.ai.ui;

import ch.so.agi.dbeaver.ai.chat.PromptAugmentation;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

final class SqlPromptInjectionResolver {
    private static final Log LOG = Log.getLog(SqlPromptInjectionResolver.class);
    private static final Pattern SQL_TOKEN_PATTERN = Pattern.compile("(?<![\\p{Alnum}_])@sql(?![\\p{Alnum}_])");

    PromptAugmentation resolve(String rawUserPrompt) {
        return resolve(rawUserPrompt, loadCurrentEditorContent());
    }

    PromptAugmentation resolve(String rawUserPrompt, SqlEditorContent editorContent) {
        String safeRawPrompt = rawUserPrompt == null ? "" : rawUserPrompt;
        if (!containsSqlToken(safeRawPrompt)) {
            return PromptAugmentation.raw(safeRawPrompt);
        }

        String normalizedPrompt = normalizePromptAfterTokenRemoval(safeRawPrompt);
        String sqlContextBlock = sqlContextBlock(editorContent);
        if (sqlContextBlock.isBlank()) {
            return new PromptAugmentation(
                safeRawPrompt,
                normalizedPrompt,
                "",
                List.of("`@sql` konnte nicht aufgeloest werden: kein aktiver SQL-Editor oder keine Query gefunden.")
            );
        }

        return new PromptAugmentation(safeRawPrompt, normalizedPrompt, sqlContextBlock, List.of());
    }

    boolean containsSqlToken(String text) {
        return text != null && SQL_TOKEN_PATTERN.matcher(text).find();
    }

    private String normalizePromptAfterTokenRemoval(String rawUserPrompt) {
        String withoutToken = SQL_TOKEN_PATTERN.matcher(rawUserPrompt == null ? "" : rawUserPrompt).replaceAll(" ");
        String compactSpaces = withoutToken.replaceAll("[\\t\\x0B\\f ]+", " ");
        String normalizedLineSpacing = compactSpaces.replaceAll(" *\n *", "\n");
        String normalizedPunctuation = normalizedLineSpacing
            .replaceAll(" +([,.;:!?\\)\\]\\}])", "$1")
            .replaceAll("([\\(\\[\\{]) +", "$1");
        return normalizedPunctuation.replaceAll("\n{3,}", "\n\n").trim();
    }

    private String sqlContextBlock(SqlEditorContent editorContent) {
        List<String> queries = new ArrayList<>();
        if (editorContent != null) {
            for (String query : editorContent.selectedQueries()) {
                String normalized = normalizeQuery(query);
                if (!normalized.isBlank()) {
                    queries.add(normalized);
                }
            }
            if (queries.isEmpty()) {
                String activeQuery = normalizeQuery(editorContent.activeQuery());
                if (!activeQuery.isBlank()) {
                    queries.add(activeQuery);
                }
            }
        }

        if (queries.isEmpty()) {
            return "";
        }
        if (queries.size() == 1) {
            return queries.get(0);
        }

        StringBuilder sqlBlock = new StringBuilder();
        for (int i = 0; i < queries.size(); i++) {
            if (i > 0) {
                sqlBlock.append("\n\n");
            }
            sqlBlock.append("-- Query ").append(i + 1).append("\n");
            sqlBlock.append(queries.get(i));
        }
        return sqlBlock.toString();
    }

    private String normalizeQuery(String query) {
        return query == null ? "" : query.trim();
    }

    private SqlEditorContent loadCurrentEditorContent() {
        SQLEditor sqlEditor = activeSqlEditor();
        if (sqlEditor == null) {
            return SqlEditorContent.empty();
        }

        List<String> selectedQueries = new ArrayList<>();
        try {
            List<SQLScriptElement> selected = sqlEditor.getSelectedQueries();
            if (selected != null) {
                for (SQLScriptElement scriptElement : selected) {
                    String text = textOf(scriptElement);
                    if (!text.isBlank()) {
                        selectedQueries.add(text);
                    }
                }
            }
        } catch (Exception ex) {
            LOG.debug("Failed to resolve selected SQL queries for @sql", ex);
        }

        String activeQuery = "";
        if (selectedQueries.isEmpty()) {
            try {
                activeQuery = textOf(sqlEditor.extractActiveQuery());
            } catch (Exception ex) {
                LOG.debug("Failed to resolve active SQL query for @sql", ex);
            }
        }

        return new SqlEditorContent(selectedQueries, activeQuery);
    }

    private SQLEditor activeSqlEditor() {
        try {
            if (!PlatformUI.isWorkbenchRunning()) {
                return null;
            }
            IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
            if (window == null) {
                return null;
            }
            IWorkbenchPage page = window.getActivePage();
            if (page == null) {
                return null;
            }
            IEditorPart editorPart = page.getActiveEditor();
            if (editorPart instanceof SQLEditor sqlEditor) {
                return sqlEditor;
            }
            return editorPart == null ? null : editorPart.getAdapter(SQLEditor.class);
        } catch (Exception ex) {
            LOG.debug("Failed to resolve active SQL editor for @sql", ex);
            return null;
        }
    }

    private String textOf(SQLScriptElement scriptElement) {
        if (scriptElement == null) {
            return "";
        }
        String text = scriptElement.getText();
        if (text == null || text.isBlank()) {
            text = scriptElement.getOriginalText();
        }
        return text == null ? "" : text.trim();
    }

    record SqlEditorContent(List<String> selectedQueries, String activeQuery) {
        SqlEditorContent {
            selectedQueries = selectedQueries == null ? List.of() : List.copyOf(selectedQueries);
            activeQuery = activeQuery == null ? "" : activeQuery;
        }

        static SqlEditorContent empty() {
            return new SqlEditorContent(List.of(), "");
        }
    }
}
