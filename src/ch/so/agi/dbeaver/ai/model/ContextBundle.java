package ch.so.agi.dbeaver.ai.model;

import java.util.List;

public record ContextBundle(List<TableContext> tableContexts, boolean truncated, List<String> warnings) {
    public ContextBundle {
        tableContexts = List.copyOf(tableContexts);
        warnings = List.copyOf(warnings);
    }
}
