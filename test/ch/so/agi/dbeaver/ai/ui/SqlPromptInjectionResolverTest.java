package ch.so.agi.dbeaver.ai.ui;

import ch.so.agi.dbeaver.ai.chat.PromptAugmentation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SqlPromptInjectionResolverTest {
    private final SqlPromptInjectionResolver resolver = new SqlPromptInjectionResolver();

    @Test
    void resolveWithoutSqlTokenLeavesPromptUntouched() {
        PromptAugmentation augmentation = resolver.resolve(
            "Analysiere #db.s.users",
            SqlPromptInjectionResolver.SqlEditorContent.empty()
        );

        assertThat(augmentation.rawUserPrompt()).isEqualTo("Analysiere #db.s.users");
        assertThat(augmentation.normalizedUserPrompt()).isEqualTo("Analysiere #db.s.users");
        assertThat(augmentation.sqlContextBlock()).isEmpty();
        assertThat(augmentation.warnings()).isEmpty();
    }

    @Test
    void resolveUsesSelectedQueriesBeforeActiveQuery() {
        PromptAugmentation augmentation = resolver.resolve(
            "Analysiere @sql",
            new SqlPromptInjectionResolver.SqlEditorContent(
                List.of("SELECT * FROM a;", "SELECT * FROM b;"),
                "SELECT * FROM ignored;"
            )
        );

        assertThat(augmentation.rawUserPrompt()).isEqualTo("Analysiere @sql");
        assertThat(augmentation.normalizedUserPrompt()).isEqualTo("Analysiere");
        assertThat(augmentation.sqlContextBlock()).contains("-- Query 1");
        assertThat(augmentation.sqlContextBlock()).contains("SELECT * FROM a;");
        assertThat(augmentation.sqlContextBlock()).contains("-- Query 2");
        assertThat(augmentation.sqlContextBlock()).contains("SELECT * FROM b;");
    }

    @Test
    void resolveFallsBackToActiveQueryWhenNothingIsSelected() {
        PromptAugmentation augmentation = resolver.resolve(
            "Analysiere @sql",
            new SqlPromptInjectionResolver.SqlEditorContent(List.of(), "SELECT * FROM users;")
        );

        assertThat(augmentation.normalizedUserPrompt()).isEqualTo("Analysiere");
        assertThat(augmentation.sqlContextBlock()).isEqualTo("SELECT * FROM users;");
        assertThat(augmentation.warnings()).isEmpty();
    }

    @Test
    void resolveDeduplicatesMultipleSqlTokensIntoSingleInjection() {
        PromptAugmentation augmentation = resolver.resolve(
            "@sql Bitte pruefe @sql",
            new SqlPromptInjectionResolver.SqlEditorContent(List.of(), "SELECT 1;")
        );

        assertThat(augmentation.normalizedUserPrompt()).isEqualTo("Bitte pruefe");
        assertThat(augmentation.sqlContextBlock()).isEqualTo("SELECT 1;");
    }

    @Test
    void resolveAddsWarningWhenNoSqlEditorQueryIsAvailable() {
        PromptAugmentation augmentation = resolver.resolve(
            "Analysiere @sql",
            SqlPromptInjectionResolver.SqlEditorContent.empty()
        );

        assertThat(augmentation.normalizedUserPrompt()).isEqualTo("Analysiere");
        assertThat(augmentation.sqlContextBlock()).isEmpty();
        assertThat(augmentation.warnings()).singleElement().satisfies(warning -> assertThat(warning).contains("@sql"));
    }
}
