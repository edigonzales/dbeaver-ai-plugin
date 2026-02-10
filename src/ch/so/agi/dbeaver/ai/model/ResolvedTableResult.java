package ch.so.agi.dbeaver.ai.model;

import java.util.List;

public record ResolvedTableResult(List<ResolvedTable> resolvedTables, List<String> warnings) {
    public ResolvedTableResult {
        resolvedTables = List.copyOf(resolvedTables);
        warnings = List.copyOf(warnings);
    }
}
