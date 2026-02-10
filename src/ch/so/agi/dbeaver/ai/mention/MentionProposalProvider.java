package ch.so.agi.dbeaver.ai.mention;

import ch.so.agi.dbeaver.ai.model.TableReference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

public final class MentionProposalProvider {
    private final Supplier<List<TableReference>> candidatesSupplier;

    public MentionProposalProvider(Supplier<List<TableReference>> candidatesSupplier) {
        this.candidatesSupplier = candidatesSupplier;
    }

    public List<MentionProposal> suggest(String prefix) {
        String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        List<MentionProposal> proposals = new ArrayList<>();

        for (TableReference candidate : candidatesSupplier.get()) {
            String plain = candidate.canonicalId();
            if (!plain.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                continue;
            }

            String display = "#" + plain;
            String insert = plain.substring(Math.min(normalizedPrefix.length(), plain.length()));
            proposals.add(new MentionProposal(display, insert));
        }

        proposals.sort(Comparator.comparing(MentionProposal::displayText));
        return proposals;
    }
}
