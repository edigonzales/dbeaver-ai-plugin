package ch.so.agi.dbeaver.ai.context;

import ch.so.agi.dbeaver.ai.model.ResolvedTable;
import ch.so.agi.dbeaver.ai.model.TableReference;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DBeaverTableDdlExtractorTest {
    @Test
    void fallsBackWhenNoNativeEntityAvailable() throws Exception {
        DBeaverTableDdlExtractor extractor = new DBeaverTableDdlExtractor();
        ResolvedTable resolved = new ResolvedTable(
            new TableReference("db", "s", "t", "#db.s.t"),
            "db.s.t",
            null,
            null
        );

        String ddl = extractor.extractDdl(resolved);

        assertThat(ddl).contains("CREATE TABLE db.s.t");
    }
}
