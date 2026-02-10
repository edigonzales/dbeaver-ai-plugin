package ch.so.agi.dbeaver.ai.mention;

import ch.so.agi.dbeaver.ai.model.TableReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MentionProposalProviderTest {
    @Test
    void suggestsDatasourceNamesForDatasourcePrefix() {
        MentionProposalProvider provider = new MentionProposalProvider(() -> List.of(
            new TableReference("alpha_db", "schema", "table_one", "#alpha_db.schema.table_one"),
            new TableReference("analytics_db", "schema", "table_two", "#analytics_db.schema.table_two"),
            new TableReference("beta_db", "schema", "table_three", "#beta_db.schema.table_three")
        ));

        List<MentionProposal> proposals = provider.suggest("a");

        assertThat(proposals).hasSize(2);
        assertThat(proposals).extracting(MentionProposal::displayText)
            .containsExactly("#alpha_db", "#analytics_db");
    }

    @Test
    void suggestsSchemasForSelectedDatasource() {
        MentionProposalProvider provider = new MentionProposalProvider(() -> List.of(
            new TableReference("db", "core", "users", "#db.core.users"),
            new TableReference("db", "sales", "orders", "#db.sales.orders"),
            new TableReference("other", "core", "users", "#other.core.users")
        ));

        List<MentionProposal> proposals = provider.suggest("db.");

        assertThat(proposals).extracting(MentionProposal::displayText)
            .containsExactly("core", "sales");
        assertThat(proposals).extracting(MentionProposal::insertText)
            .containsExactly("core", "sales");
    }

    @Test
    void suggestsSchemasAfterDatasourceWithDots() {
        MentionProposalProvider provider = new MentionProposalProvider(() -> List.of(
            new TableReference("ch.so.afu.abbaustellen.gpkg", "main", "users", "#ch.so.afu.abbaustellen.gpkg.main.users"),
            new TableReference("ch.so.afu.abbaustellen.gpkg", "public", "orders", "#ch.so.afu.abbaustellen.gpkg.public.orders"),
            new TableReference("other", "main", "x", "#other.main.x")
        ));

        List<MentionProposal> proposals = provider.suggest("ch.so.afu.abbaustellen.gpkg.");

        assertThat(proposals).extracting(MentionProposal::displayText)
            .containsExactly("main", "public");
    }

    @Test
    void suggestsTablesForSelectedDatasourceAndSchemaWithSuffixInsertion() {
        MentionProposalProvider provider = new MentionProposalProvider(() -> List.of(
            new TableReference("db", "sales", "table_one", "#db.sales.table_one"),
            new TableReference("db", "sales", "table_two", "#db.sales.table_two"),
            new TableReference("db", "core", "table_three", "#db.core.table_three")
        ));

        List<MentionProposal> proposals = provider.suggest("db.sales.ta");

        assertThat(proposals).hasSize(2);
        assertThat(proposals).extracting(MentionProposal::displayText)
            .containsExactly("table_one", "table_two");
        assertThat(proposals).extracting(MentionProposal::insertText)
            .containsExactly("ble_one", "ble_two");
    }
}
