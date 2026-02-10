package ch.so.agi.dbeaver.ai.context;

import ch.so.agi.dbeaver.ai.model.ResolvedTable;
import ch.so.agi.dbeaver.ai.model.ResolvedTableResult;
import ch.so.agi.dbeaver.ai.model.TableReference;
import ch.so.agi.dbeaver.ai.model.TableSampleRow;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContextEnricherTest {
    @Test
    void carriesResolverWarningsForUnknownTables() {
        ContextEnricher enricher = new ContextEnricher(
            refs -> new ResolvedTableResult(List.of(), List.of("Unknown table reference: db.s.missing")),
            resolved -> "DDL",
            new StubSampleCollector(),
            new SensitiveDataMasker()
        );

        var bundle = enricher.build(List.of(new TableReference("db", "s", "missing", "#db.s.missing")), 8, 5, 30, true, true);

        assertThat(bundle.tableContexts()).isEmpty();
        assertThat(bundle.warnings()).contains("Unknown table reference: db.s.missing");
    }

    @Test
    void buildsContextAndMasksSensitiveValues() {
        TableReference ref = new TableReference("db", "s", "users", "#db.s.users");
        ResolvedTable resolved = new ResolvedTable(ref, "db.s.users", new Object(), new Object());

        ContextEnricher enricher = new ContextEnricher(
            refs -> new ResolvedTableResult(List.of(resolved), List.of()),
            table -> "CREATE TABLE users(id int, password text);",
            new StubSampleCollector(),
            new SensitiveDataMasker()
        );

        var bundle = enricher.build(List.of(ref), 8, 5, 30, true, true);

        assertThat(bundle.tableContexts()).hasSize(1);
        assertThat(bundle.tableContexts().get(0).ddl()).contains("CREATE TABLE users");
        assertThat(bundle.tableContexts().get(0).sampleRows()).hasSize(1);
        assertThat(bundle.tableContexts().get(0).sampleRows().get(0).values()).containsEntry("password", "***");
    }

    private static final class StubSampleCollector implements SampleRowsCollector {
        @Override
        public List<TableSampleRow> collect(ResolvedTable resolvedTable, int maxRows, int maxColumns) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("id", "1");
            row.put("password", "top-secret");
            return List.of(new TableSampleRow(row));
        }

        @Override
        public String createSampleQueryText(ResolvedTable resolvedTable, int maxRows) {
            return "SELECT * FROM " + resolvedTable.fullyQualifiedName() + " LIMIT " + maxRows + ";";
        }
    }
}
