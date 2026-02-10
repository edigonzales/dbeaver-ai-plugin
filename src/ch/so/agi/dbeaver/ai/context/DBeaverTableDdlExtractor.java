package ch.so.agi.dbeaver.ai.context;

import ch.so.agi.dbeaver.ai.model.ResolvedTable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBStructUtils;

import java.util.LinkedHashMap;
import java.util.List;

public final class DBeaverTableDdlExtractor implements TableDdlExtractor {
    private static final Log LOG = Log.getLog(DBeaverTableDdlExtractor.class);

    @Override
    public String extractDdl(ResolvedTable resolvedTable) throws Exception {
        if (!(resolvedTable.nativeTable() instanceof DBSEntity entity)) {
            return fallbackByNameOnly(resolvedTable.fullyQualifiedName());
        }

        DBRProgressMonitor monitor = new VoidProgressMonitor();

        String ddl = null;
        try {
            ddl = DBStructUtils.generateObjectDDL(monitor, entity, new LinkedHashMap<>(), true);
        } catch (Exception ex) {
            LOG.debug("generateObjectDDL failed, trying getTableDDL fallback", ex);
        }

        if (ddl == null || ddl.isBlank()) {
            try {
                ddl = DBStructUtils.getTableDDL(monitor, entity, new LinkedHashMap<>(), true);
            } catch (Exception ex) {
                LOG.debug("getTableDDL failed, using metadata fallback", ex);
            }
        }

        if (ddl != null && !ddl.isBlank()) {
            return ddl.trim();
        }

        return fallbackFromMetadata(entity, resolvedTable.fullyQualifiedName(), monitor);
    }

    private String fallbackFromMetadata(DBSEntity entity, String fallbackName, DBRProgressMonitor monitor) {
        String tableName = fallbackName;
        try {
            tableName = DBUtils.getQuotedIdentifier(entity);
        } catch (Exception ignored) {
            // fallbackName remains.
        }

        StringBuilder sb = new StringBuilder();
        sb.append("CREATE TABLE ").append(tableName).append(" (\n");

        try {
            List<? extends DBSEntityAttribute> attributes = entity.getAttributes(monitor);
            if (attributes == null || attributes.isEmpty()) {
                sb.append("  -- no columns available\n");
            } else {
                for (int i = 0; i < attributes.size(); i++) {
                    DBSEntityAttribute attr = attributes.get(i);
                    if (i > 0) {
                        sb.append(",\n");
                    }
                    String colName = safeColumnName(attr);
                    String typeName = attr.getFullTypeName();
                    if (typeName == null || typeName.isBlank()) {
                        typeName = attr.getTypeName();
                    }
                    if (typeName == null || typeName.isBlank()) {
                        typeName = "UNKNOWN";
                    }

                    sb.append("  ").append(colName).append(' ').append(typeName);
                    if (attr.isRequired()) {
                        sb.append(" NOT NULL");
                    }
                }
                sb.append('\n');
            }
        } catch (DBException ex) {
            sb.append("  -- metadata unavailable: ").append(ex.getMessage()).append('\n');
        }

        sb.append(");");
        return sb.toString();
    }

    private String fallbackByNameOnly(String fqn) {
        return "CREATE TABLE " + fqn + " (\n  -- metadata unavailable\n);";
    }

    private String safeColumnName(DBSEntityAttribute attr) {
        try {
            return DBUtils.getQuotedIdentifier(attr);
        } catch (Exception e) {
            return attr.getName();
        }
    }
}
