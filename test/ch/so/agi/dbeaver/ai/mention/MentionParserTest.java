package ch.so.agi.dbeaver.ai.mention;

import ch.so.agi.dbeaver.ai.model.TableReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MentionParserTest {
    private final MentionParser parser = new MentionParser();

    @Test
    void parsesSimpleReference() {
        List<TableReference> refs = parser.parseReferences("Bitte prüfe #db1.public.customer");

        assertThat(refs).hasSize(1);
        assertThat(refs.get(0).datasourceName()).isEqualTo("db1");
        assertThat(refs.get(0).schemaName()).isEqualTo("public");
        assertThat(refs.get(0).tableName()).isEqualTo("customer");
    }

    @Test
    void parsesQuotedIdentifiersAndEscapedQuotes() {
        List<TableReference> refs = parser.parseReferences("Use #\"Main DB\".\"Sales\".\"Order\"\"Items\"");

        assertThat(refs).hasSize(1);
        TableReference ref = refs.get(0);
        assertThat(ref.datasourceName()).isEqualTo("Main DB");
        assertThat(ref.schemaName()).isEqualTo("Sales");
        assertThat(ref.tableName()).isEqualTo("Order\"Items");
    }

    @Test
    void deduplicatesMentions() {
        List<TableReference> refs = parser.parseReferences("#db.s.t und nochmal #db.s.t");

        assertThat(refs).hasSize(1);
        assertThat(refs.get(0).canonicalId()).isEqualTo("db.s.t");
    }

    @Test
    void ignoresTrailingPunctuation() {
        List<TableReference> refs = parser.parseReferences("Vergleiche #db.s.t, dann #db.s.u;");

        assertThat(refs).extracting(TableReference::canonicalId)
            .containsExactly("db.s.t", "db.s.u");
    }

    @Test
    void acceptsTrailingDotAfterMention() {
        List<TableReference> refs = parser.parseReferences("Zaehle #edit-dev.agi_kartenkatalog_v2.kartenkatalog_ebene.");

        assertThat(refs).hasSize(1);
        assertThat(refs.get(0).canonicalId()).isEqualTo("edit-dev.agi_kartenkatalog_v2.kartenkatalog_ebene");
    }

    @Test
    void ignoresInvalidTokens() {
        List<TableReference> refs = parser.parseReferences("#db.schema #incomplete #too.many.parts.here");

        assertThat(refs).isEmpty();
    }
}
