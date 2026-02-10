package ch.so.agi.dbeaver.ai.context;

import ch.so.agi.dbeaver.ai.model.ResolvedTable;

public interface TableDdlExtractor {
    String extractDdl(ResolvedTable resolvedTable) throws Exception;
}
