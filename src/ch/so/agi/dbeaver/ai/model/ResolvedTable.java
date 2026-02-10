package ch.so.agi.dbeaver.ai.model;

import java.util.Objects;

public record ResolvedTable(TableReference reference, String fullyQualifiedName, Object nativeTable, Object nativeExecutionContext) {
    public ResolvedTable {
        Objects.requireNonNull(reference, "reference");
        Objects.requireNonNull(fullyQualifiedName, "fullyQualifiedName");
    }
}
