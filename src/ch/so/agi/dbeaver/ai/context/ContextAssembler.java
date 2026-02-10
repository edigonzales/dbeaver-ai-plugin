package ch.so.agi.dbeaver.ai.context;

import ch.so.agi.dbeaver.ai.model.ContextBundle;
import ch.so.agi.dbeaver.ai.model.TableContext;
import ch.so.agi.dbeaver.ai.model.TableSampleRow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ContextAssembler {
    private final PromptBudgetEstimator budgetEstimator;

    public ContextAssembler(PromptBudgetEstimator budgetEstimator) {
        this.budgetEstimator = budgetEstimator;
    }

    public String toPromptBlock(ContextBundle bundle) {
        StringBuilder sb = new StringBuilder();
        sb.append("Kontextquellen:\n");
        appendDatabaseTypes(sb, bundle.tableContexts());

        for (TableContext ctx : bundle.tableContexts()) {
            sb.append("\n### Tabelle: ").append(ctx.fullyQualifiedName());
            sb.append(" (Mention: ").append(ctx.reference().rawToken()).append(")\n");
            sb.append("Datenbanktyp: ").append(ctx.databaseType()).append("\n");
            sb.append("DDL:\n");
            appendSqlCodeBlock(sb, ctx.ddl());
            sb.append("Sample Query:\n");
            appendSqlCodeBlock(sb, ctx.sampleSql());
            sb.append("Sample Rows:\n");
            if (ctx.sampleRows().isEmpty()) {
                sb.append("- <no rows>\n");
            } else {
                for (TableSampleRow row : ctx.sampleRows()) {
                    sb.append("- ").append(formatRow(row.values())).append('\n');
                }
            }
        }

        if (!bundle.warnings().isEmpty()) {
            sb.append("\nWarnings:\n");
            for (String warning : bundle.warnings()) {
                sb.append("- ").append(warning).append('\n');
            }
        }

        if (bundle.truncated()) {
            sb.append("\nNote: context was truncated due to token budget constraints.\n");
        }

        return sb.toString();
    }

    public ContextBundle truncateToBudget(ContextBundle source, int maxTokens) {
        if (maxTokens <= 0) {
            return new ContextBundle(List.of(), true, source.warnings());
        }

        List<TableContext> selected = new ArrayList<>();
        int used = 0;

        for (TableContext ctx : source.tableContexts()) {
            int next = budgetEstimator.estimateTokens(ctx.ddl())
                + budgetEstimator.estimateTokens(ctx.sampleSql())
                + estimateRows(ctx.sampleRows())
                + budgetEstimator.estimateTokens(ctx.fullyQualifiedName());

            if (used + next > maxTokens) {
                return new ContextBundle(selected, true, source.warnings());
            }

            selected.add(ctx);
            used += next;
        }

        return new ContextBundle(selected, source.truncated(), source.warnings());
    }

    private int estimateRows(List<TableSampleRow> rows) {
        int total = 0;
        for (TableSampleRow row : rows) {
            total += budgetEstimator.estimateTokens(formatRow(row.values()));
        }
        return total;
    }

    private String formatRow(Map<String, String> row) {
        StringBuilder sb = new StringBuilder("{");
        boolean first = true;
        for (Map.Entry<String, String> entry : row.entrySet()) {
            if (!first) {
                sb.append(", ");
            }
            first = false;
            sb.append(entry.getKey()).append('=').append(entry.getValue());
        }
        sb.append('}');
        return sb.toString();
    }

    private void appendSqlCodeBlock(StringBuilder sb, String sql) {
        sb.append("```sql\n");
        sb.append(sql == null ? "" : sql);
        if (sb.charAt(sb.length() - 1) != '\n') {
            sb.append('\n');
        }
        sb.append("```\n");
    }

    private void appendDatabaseTypes(StringBuilder sb, List<TableContext> contexts) {
        if (contexts.isEmpty()) {
            return;
        }

        Map<String, String> byDatasource = new LinkedHashMap<>();
        for (TableContext ctx : contexts) {
            byDatasource.putIfAbsent(ctx.reference().datasourceName(), ctx.databaseType());
        }

        sb.append("Datenbanktypen:\n");
        for (Map.Entry<String, String> entry : byDatasource.entrySet()) {
            sb.append("- ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
        }
    }
}
