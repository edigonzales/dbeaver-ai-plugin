package ch.so.agi.dbeaver.ai.mention;

public final class MentionTriggerDetector {

    public boolean isInMentionContext(String text, int caretOffset) {
        if (text == null || text.isEmpty() || caretOffset <= 0 || caretOffset > text.length()) {
            return false;
        }

        int i = caretOffset - 1;
        while (i >= 0) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                return false;
            }
            if (c == '#') {
                return true;
            }
            if (c == '\n' || c == '\r') {
                return false;
            }
            i--;
        }
        return false;
    }

    public String currentMentionPrefix(String text, int caretOffset) {
        if (!isInMentionContext(text, caretOffset)) {
            return "";
        }

        int i = caretOffset - 1;
        while (i >= 0 && text.charAt(i) != '#') {
            i--;
        }
        if (i < 0) {
            return "";
        }
        return text.substring(i + 1, caretOffset);
    }
}
