package ch.so.agi.dbeaver.ai.mention;

import ch.so.agi.dbeaver.ai.model.TableReference;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class MentionParser {

    public List<MentionToken> findMentions(String text) {
        List<MentionToken> tokens = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return tokens;
        }

        int i = 0;
        while (i < text.length()) {
            if (text.charAt(i) != '#') {
                i++;
                continue;
            }

            int start = i;
            i++;

            boolean inQuotes = false;
            while (i < text.length()) {
                char c = text.charAt(i);
                if (c == '"') {
                    if (inQuotes && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                        i += 2;
                        continue;
                    }
                    inQuotes = !inQuotes;
                    i++;
                    continue;
                }
                if (!inQuotes && (Character.isWhitespace(c) || isTokenTerminator(c))) {
                    break;
                }
                i++;
            }

            String raw = text.substring(start, i);
            if (raw.length() > 1) {
                tokens.add(new MentionToken(start, i, raw));
            }
        }

        return tokens;
    }

    private boolean isTokenTerminator(char c) {
        return c == ',' || c == ';' || c == ':' || c == '!' || c == '?' || c == ')'
            || c == ']' || c == '}' || c == '(' || c == '[' || c == '{';
    }

    public List<TableReference> parseReferences(String text) {
        List<MentionToken> tokens = findMentions(text);
        Map<String, TableReference> deduplicated = new LinkedHashMap<>();

        for (MentionToken token : tokens) {
            TableReference parsed = parseSingleToken(token.raw());
            if (parsed == null) {
                continue;
            }
            deduplicated.putIfAbsent(parsed.canonicalId(), parsed);
        }

        return new ArrayList<>(deduplicated.values());
    }

    public TableReference parseSingleToken(String rawToken) {
        if (rawToken == null || rawToken.isBlank() || rawToken.charAt(0) != '#') {
            return null;
        }

        String withoutHash = stripTrailingDots(rawToken.substring(1).trim());
        if (withoutHash.isBlank()) {
            return null;
        }
        List<String> segments = splitToken(withoutHash);
        if (segments.size() != 3) {
            return null;
        }

        String datasource = unquote(segments.get(0));
        String schema = unquote(segments.get(1));
        String table = unquote(segments.get(2));

        if (datasource.isBlank() || schema.isBlank() || table.isBlank()) {
            return null;
        }

        return new TableReference(datasource, schema, table, rawToken);
    }

    private List<String> splitToken(String token) {
        List<String> segments = new ArrayList<>(3);
        StringBuilder current = new StringBuilder();

        boolean inQuotes = false;
        for (int i = 0; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < token.length() && token.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                    current.append(c);
                }
            } else if (c == '.' && !inQuotes) {
                segments.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        segments.add(current.toString());
        return segments;
    }

    private String unquote(String value) {
        String trimmed = value.trim();
        if (trimmed.length() >= 2 && trimmed.startsWith("\"") && trimmed.endsWith("\"")) {
            String inner = trimmed.substring(1, trimmed.length() - 1);
            return inner.replace("\"\"", "\"");
        }
        return trimmed;
    }

    private String stripTrailingDots(String value) {
        int end = value.length();
        while (end > 0 && value.charAt(end - 1) == '.') {
            end--;
        }
        return value.substring(0, end);
    }
}
