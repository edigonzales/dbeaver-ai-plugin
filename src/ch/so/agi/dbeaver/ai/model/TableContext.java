package ch.so.agi.dbeaver.ai.model;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class TableContext {
    private final TableReference reference;
    private final String fullyQualifiedName;
    private final String ddl;
    private final String sampleSql;
    private final List<TableSampleRow> sampleRows;

    public TableContext(
        TableReference reference,
        String fullyQualifiedName,
        String ddl,
        String sampleSql,
        List<TableSampleRow> sampleRows
    ) {
        this.reference = Objects.requireNonNull(reference, "reference");
        this.fullyQualifiedName = Objects.requireNonNull(fullyQualifiedName, "fullyQualifiedName");
        this.ddl = Objects.requireNonNull(ddl, "ddl");
        this.sampleSql = Objects.requireNonNull(sampleSql, "sampleSql");
        this.sampleRows = List.copyOf(Objects.requireNonNull(sampleRows, "sampleRows"));
    }

    public TableReference reference() {
        return reference;
    }

    public String fullyQualifiedName() {
        return fullyQualifiedName;
    }

    public String ddl() {
        return ddl;
    }

    public String sampleSql() {
        return sampleSql;
    }

    public List<TableSampleRow> sampleRows() {
        return Collections.unmodifiableList(sampleRows);
    }
}
