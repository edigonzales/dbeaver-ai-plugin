package ch.so.agi.dbeaver.ai.context;

import ch.so.agi.dbeaver.ai.model.ResolvedTable;
import ch.so.agi.dbeaver.ai.model.TableSampleRow;

import java.util.List;

public interface SampleRowsCollector {
    List<TableSampleRow> collect(ResolvedTable resolvedTable, int maxRows, int maxColumns) throws Exception;

    String createSampleQueryText(ResolvedTable resolvedTable, int maxRows);
}
