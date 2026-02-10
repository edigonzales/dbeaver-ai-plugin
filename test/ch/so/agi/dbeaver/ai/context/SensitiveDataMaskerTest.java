package ch.so.agi.dbeaver.ai.context;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveDataMaskerTest {
    private final SensitiveDataMasker masker = new SensitiveDataMasker();

    @Test
    void masksSensitiveColumns() {
        assertThat(masker.maskValue("api_token", "abcd")).isEqualTo("***");
        assertThat(masker.maskValue("password_hash", "secret")).isEqualTo("***");
        assertThat(masker.maskValue("username", "alice")).isEqualTo("alice");
    }

    @Test
    void masksRowsInPlace() {
        Map<String, String> row = new LinkedHashMap<>();
        row.put("id", "1");
        row.put("secret_key", "123");

        masker.maskRow(row);

        assertThat(row.get("id")).isEqualTo("1");
        assertThat(row.get("secret_key")).isEqualTo("***");
    }
}
