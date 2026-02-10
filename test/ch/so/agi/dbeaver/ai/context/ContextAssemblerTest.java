package ch.so.agi.dbeaver.ai.context;

import ch.so.agi.dbeaver.ai.model.ContextBundle;
import ch.so.agi.dbeaver.ai.model.TableContext;
import ch.so.agi.dbeaver.ai.model.TableReference;
import ch.so.agi.dbeaver.ai.model.TableSampleRow;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContextAssemblerTest {
    private final ContextAssembler assembler = new ContextAssembler(new PromptBudgetEstimator());

    @Test
    void truncatesContextByBudget() {
        ContextBundle source = new ContextBundle(List.of(
            tableContext("db.s.a", "CREATE TABLE a(id int);"),
            tableContext("db.s.b", "CREATE TABLE b(id int, name varchar(1000), description varchar(1000));")
        ), false, List.of("warn"));

        ContextBundle truncated = assembler.truncateToBudget(source, 30);

        assertThat(truncated.truncated()).isTrue();
        assertThat(truncated.tableContexts()).hasSize(1);
        assertThat(truncated.warnings()).containsExactly("warn");
    }

    @Test
    void rendersStructuredContextWithSqlFences() {
        ContextBundle bundle = new ContextBundle(List.of(tableContext("db.s.a", "CREATE TABLE a(id int);")), false, List.of());

        String prompt = assembler.toPromptBlock(bundle);

        assertThat(prompt).contains("Kontextquellen:");
        assertThat(prompt).contains("### Tabelle: db.s.a (Mention: #db.s.a)");
        assertThat(prompt).contains("DDL:\n```sql");
        assertThat(prompt).contains("Sample Query:\n```sql");
        assertThat(prompt).contains("Sample Rows:");
        assertThat(prompt).contains("- {id=1, name=Alice}");
    }

    @Test
    void keepsWarningsAndTruncationNote() {
        ContextBundle bundle = new ContextBundle(List.of(tableContext("db.s.a", "CREATE TABLE a(id int);")), true, List.of("x"));

        String prompt = assembler.toPromptBlock(bundle);

        assertThat(prompt).contains("Warnings:");
        assertThat(prompt).contains("- x");
        assertThat(prompt).contains("Note: context was truncated");
    }

    private TableContext tableContext(String canonical, String ddl) {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("id", "1");
        row.put("name", "Alice");

        return new TableContext(
            new TableReference("db", "s", canonical.substring(canonical.lastIndexOf('.') + 1), "#" + canonical),
            canonical,
            ddl,
            "SELECT * FROM " + canonical + " LIMIT 5;",
            List.of(new TableSampleRow(row))
        );
    }
}
