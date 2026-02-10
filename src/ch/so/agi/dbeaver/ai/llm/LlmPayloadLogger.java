package ch.so.agi.dbeaver.ai.llm;

import ch.so.agi.dbeaver.ai.model.ChatMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class LlmPayloadLogger {
    static final int PART_CHAR_LIMIT = 4_000;

    private static final Pattern AUTHORIZATION_PATTERN =
        Pattern.compile("(?i)(authorization\\s*[:=]\\s*[\"']?)([^\\s\"',;]+)");
    private static final Pattern API_KEY_PATTERN =
        Pattern.compile("(?i)(api[_-]?key\\s*[:=]\\s*[\"']?)([^\\s\"',;]+)");
    private static final Pattern TOKEN_PATTERN =
        Pattern.compile("(?i)(token\\s*[:=]\\s*[\"']?)([^\\s\"',;]+)");

    public List<String> formatRequestParts(LlmRequest request) {
        StringBuilder text = new StringBuilder();
        text.append("SYSTEM\n");
        text.append(safe(request.systemPrompt())).append("\n\n");

        text.append("HISTORY\n");
        if (request.history().isEmpty()) {
            text.append("<empty>\n\n");
        } else {
            for (int i = 0; i < request.history().size(); i++) {
                ChatMessage message = request.history().get(i);
                text.append("[").append(i + 1).append("] ").append(message.role().name()).append("\n");
                text.append(safe(message.text())).append("\n");
            }
            text.append("\n");
        }

        text.append("USER\n");
        text.append(safe(request.userPrompt())).append("\n\n");

        text.append("CONTEXT\n");
        String contextText = safe(request.contextBlock());
        text.append(contextText.isBlank() ? "<empty>" : contextText).append("\n");

        return split(maskSensitiveValues(text.toString()));
    }

    public List<String> formatResponseParts(String responseText) {
        StringBuilder text = new StringBuilder();
        text.append("ASSISTANT\n");
        String value = safe(responseText);
        text.append(value.isBlank() ? "<empty>" : value).append("\n");
        return split(maskSensitiveValues(text.toString()));
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String maskSensitiveValues(String value) {
        String masked = AUTHORIZATION_PATTERN.matcher(value).replaceAll("$1***");
        masked = API_KEY_PATTERN.matcher(masked).replaceAll("$1***");
        return TOKEN_PATTERN.matcher(masked).replaceAll("$1***");
    }

    private List<String> split(String value) {
        if (value == null || value.isEmpty()) {
            return List.of("<empty>");
        }

        List<String> parts = new ArrayList<>();
        for (int start = 0; start < value.length(); start += PART_CHAR_LIMIT) {
            int end = Math.min(value.length(), start + PART_CHAR_LIMIT);
            parts.add(value.substring(start, end));
        }
        return parts;
    }
}
