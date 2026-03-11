package ch.so.agi.dbeaver.ai.context;

import ch.so.agi.dbeaver.ai.model.ContextBundle;
import ch.so.agi.dbeaver.ai.model.ResolvedTable;
import ch.so.agi.dbeaver.ai.model.ResolvedTableResult;
import ch.so.agi.dbeaver.ai.model.TableContext;
import ch.so.agi.dbeaver.ai.model.TableReference;
import ch.so.agi.dbeaver.ai.model.TableSampleRow;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ContextEnricher {
    private static final String NO_COLUMNS_MARKER = "-- no columns available";
    private static final String METADATA_UNAVAILABLE_MARKER = "-- metadata unavailable";

    private final TableReferenceResolver resolver;
    private final TableDdlExtractor ddlExtractor;
    private final SampleRowsCollector sampleRowsCollector;
    private final SensitiveDataMasker sensitiveDataMasker;

    public ContextEnricher(
        TableReferenceResolver resolver,
        TableDdlExtractor ddlExtractor,
        SampleRowsCollector sampleRowsCollector,
        SensitiveDataMasker sensitiveDataMasker
    ) {
        this.resolver = resolver;
        this.ddlExtractor = ddlExtractor;
        this.sampleRowsCollector = sampleRowsCollector;
        this.sensitiveDataMasker = sensitiveDataMasker;
    }

    public ContextBundle build(
        List<TableReference> references,
        int maxReferencedTables,
        int sampleRowLimit,
        int maxColumns,
        boolean includeDdl,
        boolean includeSampleRows
    ) {
        ResolvedTableResult resolved = resolver.resolve(references);

        List<TableContext> tableContexts = new ArrayList<>();
        List<String> warnings = new ArrayList<>(resolved.warnings());

        int max = Math.max(0, maxReferencedTables);
        int count = 0;
        for (ResolvedTable table : resolved.resolvedTables()) {
            if (count >= max) {
                warnings.add("Maximum number of referenced tables reached: " + max);
                break;
            }

            String ddl = "<disabled>";
            if (includeDdl) {
                try {
                    ddl = ddlExtractor.extractDdl(table);
                } catch (Exception ex) {
                    ddl = "-- DDL unavailable: " + ex.getMessage();
                    warnings.add("DDL extraction failed for " + table.fullyQualifiedName() + ": " + ex.getMessage());
                }
                addDdlQualityWarning(warnings, table.fullyQualifiedName(), ddl);
            }

            String sampleSql = sampleRowsCollector.createSampleQueryText(table, sampleRowLimit);
            List<TableSampleRow> rows = List.of();
            if (includeSampleRows) {
                try {
                    rows = maskRows(sampleRowsCollector.collect(table, sampleRowLimit, maxColumns));
                } catch (Exception ex) {
                    warnings.add("Sample row collection failed for " + table.fullyQualifiedName() + ": " + ex.getMessage());
                }
            }

            tableContexts.add(new TableContext(
                table.reference(),
                table.fullyQualifiedName(),
                table.databaseType(),
                ddl,
                sampleSql,
                rows
            ));
            count++;
        }

        return new ContextBundle(tableContexts, false, warnings);
    }

    private void addDdlQualityWarning(List<String> warnings, String fullyQualifiedName, String ddl) {
        if (ddl == null || ddl.isBlank()) {
            return;
        }
        if (ddl.contains(NO_COLUMNS_MARKER)) {
            warnings.add("DDL context for " + fullyQualifiedName + " is incomplete: no column metadata available.");
        } else if (ddl.contains(METADATA_UNAVAILABLE_MARKER)) {
            warnings.add("DDL context for " + fullyQualifiedName + " is incomplete: metadata unavailable.");
        }
    }

    private List<TableSampleRow> maskRows(List<TableSampleRow> rows) {
        List<TableSampleRow> result = new ArrayList<>(rows.size());
        for (TableSampleRow row : rows) {
            Map<String, String> copied = new LinkedHashMap<>(row.values());
            sensitiveDataMasker.maskRow(copied);
            result.add(new TableSampleRow(copied));
        }
        return result;
    }
}
