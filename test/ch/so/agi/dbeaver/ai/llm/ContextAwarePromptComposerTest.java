package ch.so.agi.dbeaver.ai.llm;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ContextAwarePromptComposerTest {
    private final ContextAwarePromptComposer composer = new ContextAwarePromptComposer();

    @Test
    void compose_withTableAndSqlContext_includesInstructionAndContextSections() {
        String contextBlock = "Kontextquellen:\n### Tabelle: db.s.t";
        String sqlBlock = "SELECT * FROM users;";
        String out = composer.composeUserPrompt("Generate SQL", contextBlock, sqlBlock);

        assertThat(out).contains("## Nutzeranfrage");
        assertThat(out).contains("Generate SQL");
        assertThat(out).contains("## Arbeitsauftrag");
        assertThat(out).contains("## SQL aus dem aktiven Editor (automatisch eingefuegt)");
        assertThat(out).contains("```sql\nSELECT * FROM users;\n```");
        assertThat(out).contains("## Tabellenkontext (automatisch aus der Datenbank extrahiert)");
        assertThat(out).endsWith(contextBlock);
    }

    @Test
    void compose_withoutContext_returnsTrimmedUserInput() {
        String out = composer.composeUserPrompt("  Generate SQL  ", "", "");

        assertThat(out).isEqualTo("Generate SQL");
    }

    @Test
    void compose_withOnlySqlContext_omitsTableSection() {
        String out = composer.composeUserPrompt("  Generate SQL  ", "", "SELECT 1;");

        assertThat(out).contains("## SQL aus dem aktiven Editor (automatisch eingefuegt)");
        assertThat(out).doesNotContain("## Tabellenkontext (automatisch aus der Datenbank extrahiert)");
    }
}
