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
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class DBeaverTableDdlExtractor implements TableDdlExtractor {
    private static final Log LOG = Log.getLog(DBeaverTableDdlExtractor.class);
    private static final Pattern EMPTY_CREATE_TABLE_PATTERN = Pattern.compile(
        "(?is)^create\\s+table\\b.*?\\(\\s*(?:(?:--[^\\r\\n]*(?:\\R|$))|(?:/\\*.*?\\*/)|\\s+)*\\)\\s*;?\\s*$"
    );
    private final DdlFacade ddlFacade;

    public DBeaverTableDdlExtractor() {
        this(new DefaultDdlFacade());
    }

    DBeaverTableDdlExtractor(DdlFacade ddlFacade) {
        this.ddlFacade = Objects.requireNonNull(ddlFacade, "ddlFacade");
    }

    @Override
    public String extractDdl(ResolvedTable resolvedTable) throws Exception {
        if (!(resolvedTable.nativeTable() instanceof DBSEntity entity)) {
            return fallbackByNameOnly(resolvedTable.fullyQualifiedName());
        }

        DBRProgressMonitor monitor = new VoidProgressMonitor();

        String ddl = null;
        try {
            ddl = normalizeNativeDdl(
                ddlFacade.generateObjectDdl(monitor, entity, createDdlOptions(), false),
                "generateObjectDDL",
                resolvedTable.fullyQualifiedName()
            );
        } catch (Exception ex) {
            LOG.debug("Native DDL generation via generateObjectDDL failed, trying getTableDDL fallback", ex);
        }

        if (ddl == null) {
            LOG.debug("generateObjectDDL returned no usable DDL, trying getTableDDL fallback");
            try {
                ddl = normalizeNativeDdl(
                    ddlFacade.getTableDdl(monitor, entity, createDdlOptions(), false),
                    "getTableDDL",
                    resolvedTable.fullyQualifiedName()
                );
            } catch (Exception ex) {
                LOG.debug("Native DDL generation via getTableDDL failed, using metadata fallback", ex);
            }
        }

        if (ddl != null) {
            return ddl;
        }

        LOG.debug("No native DDL available, using metadata fallback for " + resolvedTable.fullyQualifiedName());
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

    private Map<String, Object> createDdlOptions() {
        return new LinkedHashMap<>();
    }

    private String normalizeNativeDdl(String ddl, String source, String fullyQualifiedName) {
        if (ddl == null || ddl.isBlank()) {
            LOG.debug("Native DDL from " + source + " was blank for " + fullyQualifiedName);
            return null;
        }

        String trimmed = ddl.trim();
        if (EMPTY_CREATE_TABLE_PATTERN.matcher(trimmed).matches()) {
            LOG.debug("Native DDL from " + source + " was structurally empty for " + fullyQualifiedName);
            return null;
        }

        return trimmed;
    }

    interface DdlFacade {
        String generateObjectDdl(
            DBRProgressMonitor monitor,
            DBSEntity entity,
            Map<String, Object> options,
            boolean includeHeader
        ) throws Exception;

        String getTableDdl(
            DBRProgressMonitor monitor,
            DBSEntity entity,
            Map<String, Object> options,
            boolean includeHeader
        ) throws Exception;
    }

    private static final class DefaultDdlFacade implements DdlFacade {
        @Override
        public String generateObjectDdl(
            DBRProgressMonitor monitor,
            DBSEntity entity,
            Map<String, Object> options,
            boolean includeHeader
        ) throws Exception {
            return DBStructUtils.generateObjectDDL(monitor, entity, options, includeHeader);
        }

        @Override
        public String getTableDdl(
            DBRProgressMonitor monitor,
            DBSEntity entity,
            Map<String, Object> options,
            boolean includeHeader
        ) throws Exception {
            return DBStructUtils.getTableDDL(monitor, entity, options, includeHeader);
        }
    }
}
