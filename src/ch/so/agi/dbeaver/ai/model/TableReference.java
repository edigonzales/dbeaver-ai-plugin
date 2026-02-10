package ch.so.agi.dbeaver.ai.model;

import java.util.Objects;

public record TableReference(String datasourceName, String schemaName, String tableName, String rawToken) {

    public TableReference {
        Objects.requireNonNull(datasourceName, "datasourceName");
        Objects.requireNonNull(schemaName, "schemaName");
        Objects.requireNonNull(tableName, "tableName");
        Objects.requireNonNull(rawToken, "rawToken");
    }

    public String canonicalId() {
        return datasourceName + "." + schemaName + "." + tableName;
    }
}
