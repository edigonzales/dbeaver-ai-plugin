package ch.so.agi.dbeaver.ai.context;

import ch.so.agi.dbeaver.ai.model.ResolvedTable;
import ch.so.agi.dbeaver.ai.model.TableSampleRow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCAttributeMetaData;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DBeaverSampleRowsCollector implements SampleRowsCollector {
    private static final int MAX_CELL_LENGTH = 500;

    @Override
    public List<TableSampleRow> collect(ResolvedTable resolvedTable, int maxRows, int maxColumns) throws Exception {
        int safeMaxRows = Math.max(1, maxRows);
        int safeMaxColumns = Math.max(1, maxColumns);

        DBCExecutionContext executionContext = resolveExecutionContext(resolvedTable);
        DBRProgressMonitor monitor = new VoidProgressMonitor();

        String query = createSampleQueryText(resolvedTable, safeMaxRows);
        List<TableSampleRow> rows = new ArrayList<>();

        try (DBCSession session = executionContext.openSession(monitor, DBCExecutionPurpose.USER, "AI sample row fetch");
             DBCStatement statement = session.prepareStatement(DBCStatementType.QUERY, query, false, false, false)) {
            statement.setLimit(0, safeMaxRows);

            if (!statement.executeStatement()) {
                return rows;
            }

            try (DBCResultSet resultSet = statement.openResultSet()) {
                if (resultSet == null || resultSet.getMeta() == null || resultSet.getMeta().getAttributes() == null) {
                    return rows;
                }

                List<? extends DBCAttributeMetaData> attributes = resultSet.getMeta().getAttributes();
                while (rows.size() < safeMaxRows && resultSet.nextRow()) {
                    Map<String, String> values = new LinkedHashMap<>();
                    int colCount = Math.min(attributes.size(), safeMaxColumns);
                    for (int i = 0; i < colCount; i++) {
                        DBCAttributeMetaData attr = attributes.get(i);
                        String name = attr.getName() == null || attr.getName().isBlank() ? "col_" + (i + 1) : attr.getName();
                        Object raw = resultSet.getAttributeValue(i);
                        values.put(name, formatValue(raw));
                    }
                    rows.add(new TableSampleRow(values));
                }
            }
        }

        return rows;
    }

    @Override
    public String createSampleQueryText(ResolvedTable resolvedTable, int maxRows) {
        int safeMaxRows = Math.max(1, maxRows);
        return "SELECT * FROM " + resolvedTable.fullyQualifiedName() + " LIMIT " + safeMaxRows + ";";
    }

    private DBCExecutionContext resolveExecutionContext(ResolvedTable resolvedTable) throws DBException {
        if (resolvedTable.nativeExecutionContext() instanceof DBCExecutionContext context) {
            return context;
        }

        if (resolvedTable.nativeTable() instanceof DBSEntity entity) {
            return DBUtils.getOrOpenDefaultContext(entity, false);
        }

        throw new DBException("No execution context available for " + resolvedTable.fullyQualifiedName());
    }

    private String formatValue(Object raw) {
        if (raw == null) {
            return "NULL";
        }

        String text = String.valueOf(raw);
        if (text.length() > MAX_CELL_LENGTH) {
            return text.substring(0, MAX_CELL_LENGTH) + "...";
        }
        return text;
    }
}
