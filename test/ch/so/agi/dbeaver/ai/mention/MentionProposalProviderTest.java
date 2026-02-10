package ch.so.agi.dbeaver.ai.mention;

import ch.so.agi.dbeaver.ai.model.TableReference;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MentionProposalProviderTest {
    @Test
    void returnsSuffixForInsertion() {
        MentionProposalProvider provider = new MentionProposalProvider(() -> List.of(
            new TableReference("db", "schema", "table_one", "#db.schema.table_one"),
            new TableReference("db", "schema", "table_two", "#db.schema.table_two")
        ));

        List<MentionProposal> proposals = provider.suggest("db.schema.ta");

        assertThat(proposals).hasSize(2);
        assertThat(proposals.get(0).displayText()).isEqualTo("#db.schema.table_one");
        assertThat(proposals.get(0).insertText()).isEqualTo("ble_one");
    }
}
