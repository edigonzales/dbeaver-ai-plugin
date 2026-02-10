package ch.so.agi.dbeaver.ai.context;

import ch.so.agi.dbeaver.ai.model.ResolvedTable;
import ch.so.agi.dbeaver.ai.model.ResolvedTableResult;
import ch.so.agi.dbeaver.ai.model.TableReference;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public final class DBeaverTableReferenceResolver implements TableReferenceResolver {
    private static final Log LOG = Log.getLog(DBeaverTableReferenceResolver.class);
    private static final int MAX_TRAVERSAL_DEPTH = 5;

    @Override
    public ResolvedTableResult resolve(List<TableReference> references) {
        List<ResolvedTable> resolved = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        DBRProgressMonitor monitor = new VoidProgressMonitor();
        for (TableReference reference : references) {
            try {
                Resolution resolution = resolveSingle(reference, monitor);
                if (resolution.table == null) {
                    warnings.add("Unknown table reference: " + reference.canonicalId());
                    continue;
                }
                resolved.add(new ResolvedTable(reference, resolution.fqn, resolution.table, resolution.executionContext));
            } catch (Exception e) {
                warnings.add("Failed to resolve reference " + reference.canonicalId() + ": " + e.getMessage());
                LOG.debug("Failed to resolve table reference", e);
            }
        }

        return new ResolvedTableResult(resolved, warnings);
    }

    private Resolution resolveSingle(TableReference reference, DBRProgressMonitor monitor) throws DBException {
        DBPDataSourceContainer container = findDataSourceContainer(reference.datasourceName());
        if (container == null) {
            return Resolution.unresolved();
        }

        if (!container.isConnected()) {
            container.connect(monitor, false, false);
        }

        DBPDataSource dataSource = container.getDataSource();
        if (dataSource == null) {
            return Resolution.unresolved();
        }

        DBCExecutionContext executionContext = DBUtils.getOrOpenDefaultContext(dataSource, false);
        DBSEntity table = findEntity(dataSource, reference.schemaName(), reference.tableName(), monitor);
        if (table == null) {
            return Resolution.unresolved();
        }

        String fqn = DBUtils.getObjectFullName(dataSource, table, DBPEvaluationContext.DML);
        return new Resolution(table, executionContext, fqn);
    }

    private DBSEntity findEntity(DBPDataSource dataSource, String schemaName, String tableName, DBRProgressMonitor monitor) {
        if (dataSource instanceof DBSEntity entity && equalsIgnoreCase(entity.getName(), tableName)) {
            return entity;
        }

        if (dataSource instanceof DBSObjectContainer container) {
            DBSEntity entity = findEntityRecursive(container, schemaName, tableName, monitor, MAX_TRAVERSAL_DEPTH, new HashSet<>());
            if (entity != null) {
                return entity;
            }
        }

        DBSObject parent = dataSource.getParentObject();
        if (parent instanceof DBSObjectContainer container) {
            return findEntityRecursive(container, schemaName, tableName, monitor, MAX_TRAVERSAL_DEPTH, new HashSet<>());
        }

        return null;
    }

    private DBSEntity findEntityRecursive(
        DBSObjectContainer container,
        String schemaName,
        String tableName,
        DBRProgressMonitor monitor,
        int depth,
        Set<String> visited
    ) {
        if (depth < 0) {
            return null;
        }

        String containerId = containerKey(container);
        if (!visited.add(containerId)) {
            return null;
        }

        try {
            if (equalsIgnoreCase(container.getName(), schemaName)) {
                DBSEntity tableInSchema = findEntityDirectChild(container, tableName, monitor);
                if (tableInSchema != null) {
                    return tableInSchema;
                }
            }

            DBSObject schemaObject = findChildIgnoreCase(container, schemaName, monitor);
            if (schemaObject instanceof DBSObjectContainer schemaContainer) {
                DBSEntity tableInSchema = findEntityDirectChild(schemaContainer, tableName, monitor);
                if (tableInSchema != null) {
                    return tableInSchema;
                }
            }

            Collection<? extends DBSObject> children = container.getChildren(monitor);
            if (children == null) {
                return null;
            }

            for (DBSObject child : children) {
                if (child instanceof DBSEntity entity
                    && equalsIgnoreCase(entity.getName(), tableName)
                    && child.getParentObject() != null
                    && equalsIgnoreCase(child.getParentObject().getName(), schemaName)) {
                    return entity;
                }

                if (child instanceof DBSObjectContainer nested) {
                    DBSEntity nestedResult = findEntityRecursive(nested, schemaName, tableName, monitor, depth - 1, visited);
                    if (nestedResult != null) {
                        return nestedResult;
                    }
                }
            }
        } catch (DBException e) {
            LOG.debug("Object traversal failed while resolving table", e);
        }

        return null;
    }

    private DBSEntity findEntityDirectChild(DBSObjectContainer container, String tableName, DBRProgressMonitor monitor) {
        try {
            DBSObject child = findChildIgnoreCase(container, tableName, monitor);
            if (child instanceof DBSEntity entity) {
                return entity;
            }

            Collection<? extends DBSObject> children = container.getChildren(monitor);
            if (children == null) {
                return null;
            }

            for (DBSObject object : children) {
                if (object instanceof DBSEntity entity && equalsIgnoreCase(entity.getName(), tableName)) {
                    return entity;
                }
            }
        } catch (DBException e) {
            LOG.debug("Failed to resolve direct child table", e);
        }

        return null;
    }

    private DBSObject findChildIgnoreCase(DBSObjectContainer container, String name, DBRProgressMonitor monitor) throws DBException {
        DBSObject direct = container.getChild(monitor, name);
        if (direct != null) {
            return direct;
        }

        Collection<? extends DBSObject> children = container.getChildren(monitor);
        if (children == null || children.isEmpty()) {
            return null;
        }

        for (DBSObject child : children) {
            if (equalsIgnoreCase(child.getName(), name)) {
                return child;
            }
        }
        return null;
    }

    private DBPDataSourceContainer findDataSourceContainer(String datasourceName) {
        DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
        if (workspace == null) {
            return null;
        }

        DBPProject activeProject = workspace.getActiveProject();
        DBPDataSourceContainer fromActive = findDataSourceInProject(activeProject, datasourceName);
        if (fromActive != null) {
            return fromActive;
        }

        for (DBPProject project : workspace.getProjects()) {
            if (Objects.equals(project, activeProject)) {
                continue;
            }

            DBPDataSourceContainer ds = findDataSourceInProject(project, datasourceName);
            if (ds != null) {
                return ds;
            }
        }

        return null;
    }

    private DBPDataSourceContainer findDataSourceInProject(DBPProject project, String datasourceName) {
        if (project == null) {
            return null;
        }

        DBPDataSourceRegistry registry = project.getDataSourceRegistry();
        if (registry == null) {
            return null;
        }

        DBPDataSourceContainer exact = registry.findDataSourceByName(datasourceName);
        if (exact != null) {
            return exact;
        }

        List<? extends DBPDataSourceContainer> sources = registry.getDataSources();
        if (sources == null) {
            return null;
        }

        String target = datasourceName.toLowerCase(Locale.ROOT);
        for (DBPDataSourceContainer source : sources) {
            if (source != null && source.getName() != null && source.getName().toLowerCase(Locale.ROOT).equals(target)) {
                return source;
            }
        }

        return null;
    }

    private boolean equalsIgnoreCase(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        return left.equalsIgnoreCase(right);
    }

    private String containerKey(DBSObjectContainer container) {
        try {
            String id = DBUtils.getObjectFullId(container);
            if (id != null && !id.isBlank()) {
                return id;
            }
        } catch (Exception ignored) {
            // fallback to identity hash below
        }
        return container.getClass().getName() + "@" + System.identityHashCode(container);
    }

    private static final class Resolution {
        private final DBSEntity table;
        private final DBCExecutionContext executionContext;
        private final String fqn;

        private Resolution(DBSEntity table, DBCExecutionContext executionContext, String fqn) {
            this.table = table;
            this.executionContext = executionContext;
            this.fqn = fqn;
        }

        private static Resolution unresolved() {
            return new Resolution(null, null, "");
        }
    }
}
