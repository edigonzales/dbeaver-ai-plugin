package ch.so.agi.dbeaver.ai.context;

import ch.so.agi.dbeaver.ai.model.ResolvedTableResult;
import ch.so.agi.dbeaver.ai.model.TableReference;

import java.util.List;

public interface TableReferenceResolver {
    ResolvedTableResult resolve(List<TableReference> references);
}
