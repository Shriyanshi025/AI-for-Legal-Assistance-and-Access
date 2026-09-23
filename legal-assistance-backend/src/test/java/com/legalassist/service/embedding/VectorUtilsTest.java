package com.legalassist.service.embedding;

import com.legalassist.exception.EmbeddingException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VectorUtilsTest {

    @Test
    @DisplayName("l2Normalize should produce a unit vector with L2 norm equal to 1.0")
    void l2NormalizeShouldNormalizeVector() {
        List<Float> input = List.of(3.0f, 4.0f);
        List<Float> normalized = VectorUtils.l2Normalize(input);

        assertThat(normalized).hasSize(2);
        assertThat(normalized.get(0)).isCloseTo(0.6f, org.assertj.core.data.Offset.offset(0.0001f));
        assertThat(normalized.get(1)).isCloseTo(0.8f, org.assertj.core.data.Offset.offset(0.0001f));

        double norm = Math.sqrt(normalized.get(0) * normalized.get(0) + normalized.get(1) * normalized.get(1));
        assertThat(norm).isCloseTo(1.0, org.assertj.core.data.Offset.offset(0.0001));
    }

    @Test
    @DisplayName("l2Normalize should throw EmbeddingException for zero norm vector")
    void l2NormalizeShouldRejectZeroNorm() {
        List<Float> zeroVector = List.of(0.0f, 0.0f, 0.0f);
        assertThatThrownBy(() -> VectorUtils.l2Normalize(zeroVector))
                .isInstanceOf(EmbeddingException.class)
                .hasMessageContaining("zero");
    }

    @Test
    @DisplayName("formatPgVector should format Float list into pgvector SQL string")
    void formatPgVectorShouldReturnCorrectString() {
        List<Float> vector = List.of(0.123f, -0.456f, 0.789f);
        String formatted = VectorUtils.formatPgVector(vector);

        assertThat(formatted).isEqualTo("[0.123,-0.456,0.789]");
    }

    @Test
    @DisplayName("parsePgVector should parse pgvector SQL string back into Float list")
    void parsePgVectorShouldReturnFloatList() {
        String input = "[0.123, -0.456, 0.789]";
        List<Float> parsed = VectorUtils.parsePgVector(input);

        assertThat(parsed).containsExactly(0.123f, -0.456f, 0.789f);
    }
}
