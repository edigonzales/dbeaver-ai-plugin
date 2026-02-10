package ch.so.agi.dbeaver.ai.mention;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MentionTriggerDetectorTest {
    private final MentionTriggerDetector detector = new MentionTriggerDetector();

    @Test
    void detectsMentionContext() {
        String text = "Please inspect #db.schema.ta";
        int caret = text.length();

        assertThat(detector.isInMentionContext(text, caret)).isTrue();
        assertThat(detector.currentMentionPrefix(text, caret)).isEqualTo("db.schema.ta");
    }

    @Test
    void returnsFalseWhenCaretOutsideMention() {
        String text = "Please inspect #db.schema.ta next";
        int caret = text.length();

        assertThat(detector.isInMentionContext(text, caret)).isFalse();
        assertThat(detector.currentMentionPrefix(text, caret)).isEmpty();
    }
}
