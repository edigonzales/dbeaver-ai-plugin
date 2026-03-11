package ch.so.agi.dbeaver.ai.context;

import ch.so.agi.dbeaver.ai.model.ResolvedTable;
import ch.so.agi.dbeaver.ai.model.TableReference;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DBeaverTableDdlExtractorTest {
    @Test
    void usesNativeDdlWithoutHeaderWhenEntityAvailable() throws Exception {
        RecordingFacade facade = new RecordingFacade();
        facade.generateObjectDdlResult = "  CREATE TABLE native_table(id int);  ";

        DBeaverTableDdlExtractor extractor = new DBeaverTableDdlExtractor(facade);

        String ddl = extractor.extractDdl(resolvedWithEntity(nativeEntity()));

        assertThat(ddl).isEqualTo("CREATE TABLE native_table(id int);");
        assertThat(facade.calls).containsExactly("generateObjectDdl");
        assertThat(facade.generateObjectDdlIncludeHeader).isFalse();
    }

    @Test
    void fallsBackToGetTableDdlWithoutHeaderWhenGenerateObjectDdlFails() throws Exception {
        RecordingFacade facade = new RecordingFacade();
        facade.generateObjectDdlFailure = new RuntimeException("boom");
        facade.getTableDdlResult = "CREATE TABLE fallback_table(id int);";

        DBeaverTableDdlExtractor extractor = new DBeaverTableDdlExtractor(facade);

        String ddl = extractor.extractDdl(resolvedWithEntity(nativeEntity()));

        assertThat(ddl).isEqualTo("CREATE TABLE fallback_table(id int);");
        assertThat(facade.calls).containsExactly("generateObjectDdl", "getTableDdl");
        assertThat(facade.generateObjectDdlIncludeHeader).isFalse();
        assertThat(facade.getTableDdlIncludeHeader).isFalse();
    }

    @Test
    void fallsBackWhenNoNativeEntityAvailable() throws Exception {
        DBeaverTableDdlExtractor extractor = new DBeaverTableDdlExtractor();
        ResolvedTable resolved = new ResolvedTable(
            new TableReference("db", "s", "t", "#db.s.t"),
            "db.s.t",
            "PostgreSQL",
            null,
            null
        );

        String ddl = extractor.extractDdl(resolved);

        assertThat(ddl).contains("CREATE TABLE db.s.t");
    }

    private ResolvedTable resolvedWithEntity(DBSEntity entity) {
        return new ResolvedTable(
            new TableReference("db", "s", "t", "#db.s.t"),
            "db.s.t",
            "PostgreSQL",
            entity,
            null
        );
    }

    private DBSEntity nativeEntity() {
        return (DBSEntity) Proxy.newProxyInstance(
            DBSEntity.class.getClassLoader(),
            new Class[]{DBSEntity.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "toString" -> "nativeEntity";
                case "hashCode" -> System.identityHashCode(proxy);
                case "equals" -> proxy == args[0];
                default -> defaultValue(method.getReturnType());
            }
        );
    }

    private Object defaultValue(Class<?> returnType) {
        if (!returnType.isPrimitive()) {
            return null;
        }
        if (returnType == boolean.class) {
            return false;
        }
        if (returnType == byte.class) {
            return (byte) 0;
        }
        if (returnType == short.class) {
            return (short) 0;
        }
        if (returnType == int.class) {
            return 0;
        }
        if (returnType == long.class) {
            return 0L;
        }
        if (returnType == float.class) {
            return 0f;
        }
        if (returnType == double.class) {
            return 0d;
        }
        if (returnType == char.class) {
            return '\0';
        }
        return null;
    }

    private static final class RecordingFacade implements DBeaverTableDdlExtractor.DdlFacade {
        private final List<String> calls = new ArrayList<>();
        private String generateObjectDdlResult;
        private String getTableDdlResult;
        private RuntimeException generateObjectDdlFailure;
        private RuntimeException getTableDdlFailure;
        private Boolean generateObjectDdlIncludeHeader;
        private Boolean getTableDdlIncludeHeader;

        @Override
        public String generateObjectDdl(
            DBRProgressMonitor monitor,
            DBSEntity entity,
            Map<String, Object> options,
            boolean includeHeader
        ) {
            calls.add("generateObjectDdl");
            generateObjectDdlIncludeHeader = includeHeader;
            if (generateObjectDdlFailure != null) {
                throw generateObjectDdlFailure;
            }
            return generateObjectDdlResult;
        }

        @Override
        public String getTableDdl(
            DBRProgressMonitor monitor,
            DBSEntity entity,
            Map<String, Object> options,
            boolean includeHeader
        ) {
            calls.add("getTableDdl");
            getTableDdlIncludeHeader = includeHeader;
            if (getTableDdlFailure != null) {
                throw getTableDdlFailure;
            }
            return getTableDdlResult;
        }
    }
}
