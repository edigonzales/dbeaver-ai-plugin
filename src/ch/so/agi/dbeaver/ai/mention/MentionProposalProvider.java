package ch.so.agi.dbeaver.ai.mention;

import ch.so.agi.dbeaver.ai.model.TableReference;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;

public final class MentionProposalProvider {
    private final Supplier<List<TableReference>> candidatesSupplier;

    public MentionProposalProvider(Supplier<List<TableReference>> candidatesSupplier) {
        this.candidatesSupplier = candidatesSupplier;
    }

    public List<MentionProposal> suggest(String prefix) {
        String typedPrefix = prefix == null ? "" : prefix;
        List<TableReference> candidates = candidatesSupplier.get();

        if (typedPrefix.indexOf('.') < 0) {
            return sorted(suggestDatasources(candidates, typedPrefix));
        }

        String datasource = findBestDatasource(candidates, typedPrefix);
        if (datasource == null) {
            return sorted(suggestDatasources(candidates, typedPrefix));
        }

        if (typedPrefix.length() == datasource.length()) {
            return sorted(suggestDatasources(candidates, typedPrefix));
        }

        if (typedPrefix.charAt(datasource.length()) != '.') {
            return sorted(suggestDatasources(candidates, typedPrefix));
        }

        String schemaAndMaybeTable = typedPrefix.substring(datasource.length() + 1);
        String schema = findBestSchema(candidates, datasource, schemaAndMaybeTable);
        if (schema == null || !matchesCompletedSegment(schemaAndMaybeTable, schema)) {
            return sorted(suggestSchemas(candidates, typedPrefix, datasource, schemaAndMaybeTable));
        }

        String tablePrefix = schemaAndMaybeTable.substring(schema.length() + 1);
        return sorted(suggestTables(candidates, typedPrefix, datasource, schema, tablePrefix));
    }

    private List<MentionProposal> suggestDatasources(List<TableReference> candidates, String typedPrefix) {
        Set<String> datasources = new LinkedHashSet<>();
        for (TableReference candidate : candidates) {
            if (startsWithIgnoreCase(candidate.datasourceName(), typedPrefix)) {
                datasources.add(candidate.datasourceName());
            }
        }

        List<MentionProposal> proposals = new ArrayList<>();
        for (String datasource : datasources) {
            proposals.add(buildProposal("#" + datasource, datasource, typedPrefix));
        }
        return proposals;
    }

    private List<MentionProposal> suggestSchemas(
        List<TableReference> candidates,
        String fullPrefix,
        String datasource,
        String schemaPrefix
    ) {
        Set<String> schemas = new LinkedHashSet<>();
        for (TableReference candidate : candidates) {
            if (!equalsIgnoreCase(candidate.datasourceName(), datasource)) {
                continue;
            }
            if (startsWithIgnoreCase(candidate.schemaName(), schemaPrefix)) {
                schemas.add(candidate.schemaName());
            }
        }

        List<MentionProposal> proposals = new ArrayList<>();
        for (String schema : schemas) {
            String completion = datasource + "." + schema;
            proposals.add(buildProposal(schema, completion, fullPrefix));
        }
        return proposals;
    }

    private List<MentionProposal> suggestTables(
        List<TableReference> candidates,
        String fullPrefix,
        String datasource,
        String schema,
        String tablePrefix
    ) {
        Set<String> tables = new LinkedHashSet<>();
        for (TableReference candidate : candidates) {
            if (!equalsIgnoreCase(candidate.datasourceName(), datasource)) {
                continue;
            }
            if (!equalsIgnoreCase(candidate.schemaName(), schema)) {
                continue;
            }
            if (startsWithIgnoreCase(candidate.tableName(), tablePrefix)) {
                tables.add(candidate.tableName());
            }
        }

        List<MentionProposal> proposals = new ArrayList<>();
        for (String table : tables) {
            String completion = datasource + "." + schema + "." + table;
            proposals.add(buildProposal(table, completion, fullPrefix));
        }
        return proposals;
    }

    private MentionProposal buildProposal(String display, String completion, String typedPrefix) {
        String insert = suffixForCompletion(completion, typedPrefix);
        return new MentionProposal(display, insert);
    }

    private String suffixForCompletion(String completion, String typedPrefix) {
        if (typedPrefix == null || typedPrefix.isEmpty()) {
            return completion;
        }
        int prefixLength = typedPrefix.length();
        if (prefixLength <= completion.length() && completion.regionMatches(true, 0, typedPrefix, 0, prefixLength)) {
            return completion.substring(prefixLength);
        }
        return completion;
    }

    private String findBestDatasource(List<TableReference> candidates, String fullPrefix) {
        Set<String> datasources = new LinkedHashSet<>();
        for (TableReference candidate : candidates) {
            datasources.add(candidate.datasourceName());
        }
        return findBestCompletedSegment(datasources, fullPrefix);
    }

    private String findBestSchema(List<TableReference> candidates, String datasource, String remainder) {
        Set<String> schemas = new LinkedHashSet<>();
        for (TableReference candidate : candidates) {
            if (equalsIgnoreCase(candidate.datasourceName(), datasource)) {
                schemas.add(candidate.schemaName());
            }
        }
        return findBestCompletedSegment(schemas, remainder);
    }

    private String findBestCompletedSegment(Set<String> values, String fullPrefix) {
        String best = null;
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!matchesCompletedSegment(fullPrefix, value)) {
                continue;
            }
            if (best == null || value.length() > best.length()) {
                best = value;
            }
        }
        return best;
    }

    private boolean matchesCompletedSegment(String fullPrefix, String segment) {
        if (fullPrefix == null || segment == null) {
            return false;
        }
        int segmentLength = segment.length();
        if (fullPrefix.length() <= segmentLength) {
            return false;
        }
        return fullPrefix.regionMatches(true, 0, segment, 0, segmentLength)
            && fullPrefix.charAt(segmentLength) == '.';
    }

    private List<MentionProposal> sorted(List<MentionProposal> proposals) {
        proposals.sort(Comparator.comparing(MentionProposal::displayText));
        return proposals;
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.equalsIgnoreCase(right);
    }

    private boolean startsWithIgnoreCase(String value, String prefix) {
        String safeValue = value == null ? "" : value;
        String safePrefix = prefix == null ? "" : prefix;
        return safeValue.toLowerCase(Locale.ROOT).startsWith(safePrefix.toLowerCase(Locale.ROOT));
    }
}
