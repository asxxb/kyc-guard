package io.github.kycguard.generator;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kycguard.generator.service.RandomDataPool;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class RandomDataPoolTest {

    @Test
    @DisplayName("Data pool lists are well-formed and non-empty")
    void testDataPoolsNonEmpty() {
        assertThat(RandomDataPool.FIRST_NAMES).isNotEmpty();
        assertThat(RandomDataPool.LAST_NAMES).isNotEmpty();
        assertThat(RandomDataPool.COMPANY_PREFIXES).isNotEmpty();
        assertThat(RandomDataPool.COMPANY_SUFFIXES).isNotEmpty();
        assertThat(RandomDataPool.COUNTRIES).isNotEmpty();
        assertThat(RandomDataPool.CITIES).isNotEmpty();
        assertThat(RandomDataPool.STREETS).isNotEmpty();
        assertThat(RandomDataPool.SOURCES).isNotEmpty();
        assertThat(RandomDataPool.TRANSLITERATIONS).isNotEmpty();
    }

    @Test
    @DisplayName("createFuzzyVariant produces valid variants with different strategies")
    void testCreateFuzzyVariant() {
        // Test with different seeds to hit multiple branches in createFuzzyVariant
        boolean seenDifferent = false;
        String original = "Mohammed Smith";

        for (int i = 0; i < 50; i++) {
            Random random = new Random(i);
            String variant = RandomDataPool.createFuzzyVariant(original, random);
            assertThat(variant).isNotBlank();
            if (!variant.equals(original)) {
                seenDifferent = true;
            }
        }

        assertThat(seenDifferent).isTrue();
    }
}
