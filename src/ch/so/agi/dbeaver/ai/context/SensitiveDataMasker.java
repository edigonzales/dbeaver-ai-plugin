package ch.so.agi.dbeaver.ai.context;

import java.util.Map;
import java.util.regex.Pattern;

public final class SensitiveDataMasker {
    private static final Pattern SENSITIVE_COLUMN_PATTERN = Pattern.compile("(?i).*(password|token|secret|api[_-]?key|private[_-]?key).*", Pattern.CASE_INSENSITIVE);

    public String maskValue(String columnName, String value) {
        if (columnName == null || value == null) {
            return value;
        }
        if (SENSITIVE_COLUMN_PATTERN.matcher(columnName).matches()) {
            return "***";
        }
        return value;
    }

    public Map<String, String> maskRow(Map<String, String> rowValues) {
        for (Map.Entry<String, String> entry : rowValues.entrySet()) {
            entry.setValue(maskValue(entry.getKey(), entry.getValue()));
        }
        return rowValues;
    }
}
