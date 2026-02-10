package ch.so.agi.dbeaver.ai.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public final class TableSampleRow {
    private final Map<String, String> values;

    public TableSampleRow(Map<String, String> values) {
        Objects.requireNonNull(values, "values");
        this.values = Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }

    public Map<String, String> values() {
        return values;
    }
}
