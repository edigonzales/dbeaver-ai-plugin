package ch.so.agi.dbeaver.ai.mention;

import ch.so.agi.dbeaver.ai.model.TableReference;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectContainer;
import org.jkiss.dbeaver.runtime.DBWorkbench;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class DBeaverMentionCatalog {
    private static final Log LOG = Log.getLog(DBeaverMentionCatalog.class);
    private static final int MAX_TRAVERSAL_DEPTH = 4;
    private static final int DEFAULT_MAX_CANDIDATES = 500;

    public List<TableReference> loadCandidates() {
        return loadCandidates(DEFAULT_MAX_CANDIDATES);
    }

    public List<TableReference> loadCandidates(int maxCandidates) {
        int safeMax = Math.max(1, maxCandidates);
        DBPWorkspace workspace = DBWorkbench.getPlatform().getWorkspace();
        if (workspace == null) {
            return List.of();
        }

        Map<String, TableReference> dedup = new LinkedHashMap<>();
        DBRProgressMonitor monitor = new VoidProgressMonitor();

        DBPProject active = workspace.getActiveProject();
        appendProjectReferences(active, dedup, monitor, safeMax);
        if (dedup.size() >= safeMax) {
            return new ArrayList<>(dedup.values());
        }

        for (DBPProject project : workspace.getProjects()) {
            if (Objects.equals(project, active)) {
                continue;
            }
            appendProjectReferences(project, dedup, monitor, safeMax);
            if (dedup.size() >= safeMax) {
                break;
            }
        }

        return new ArrayList<>(dedup.values());
    }

    private void appendProjectReferences(
        DBPProject project,
        Map<String, TableReference> dedup,
        DBRProgressMonitor monitor,
        int maxCandidates
    ) {
        if (project == null) {
            return;
        }

        DBPDataSourceRegistry registry = project.getDataSourceRegistry();
        if (registry == null) {
            return;
        }

        List<? extends DBPDataSourceContainer> dataSources = registry.getDataSources();
        if (dataSources == null) {
            return;
        }

        for (DBPDataSourceContainer dsContainer : dataSources) {
            if (dedup.size() >= maxCandidates) {
                return;
            }

            if (!dsContainer.isConnected()) {
                continue;
            }

            DBPDataSource dataSource = dsContainer.getDataSource();
            if (!(dataSource instanceof DBSObjectContainer root)) {
                continue;
            }

            String datasourceName = dsContainer.getName();
            appendReferencesFromRoot(root, datasourceName, dedup, monitor, maxCandidates);
        }
    }

    private void appendReferencesFromRoot(
        DBSObjectContainer root,
        String datasourceName,
        Map<String, TableReference> dedup,
        DBRProgressMonitor monitor,
        int maxCandidates
    ) {
        Deque<NodeWithDepth> stack = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        stack.push(new NodeWithDepth(root, 0));

        while (!stack.isEmpty() && dedup.size() < maxCandidates) {
            NodeWithDepth entry = stack.pop();
            DBSObjectContainer container = entry.container;

            if (entry.depth > MAX_TRAVERSAL_DEPTH) {
                continue;
            }

            String key = container.getClass().getName() + '@' + System.identityHashCode(container);
            if (!visited.add(key)) {
                continue;
            }

            try {
                for (DBSObject child : container.getChildren(monitor)) {
                    if (child instanceof DBSEntity entity) {
                        String schema = entity.getParentObject() == null ? "default" : safeName(entity.getParentObject().getName());
                        String table = safeName(entity.getName());
                        TableReference ref = new TableReference(safeName(datasourceName), schema, table,
                            "#" + safeName(datasourceName) + "." + schema + "." + table);
                        dedup.putIfAbsent(ref.canonicalId(), ref);
                        if (dedup.size() >= maxCandidates) {
                            return;
                        }
                    }

                    if (child instanceof DBSObjectContainer nested) {
                        stack.push(new NodeWithDepth(nested, entry.depth + 1));
                    }
                }
            } catch (Exception ex) {
                LOG.debug("Failed to traverse metadata for mention candidates", ex);
            }
        }
    }

    private String safeName(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        return value;
    }

    private static final class NodeWithDepth {
        private final DBSObjectContainer container;
        private final int depth;

        private NodeWithDepth(DBSObjectContainer container, int depth) {
            this.container = container;
            this.depth = depth;
        }
    }
}
