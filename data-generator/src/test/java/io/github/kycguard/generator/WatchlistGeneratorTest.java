package io.github.kycguard.generator;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kycguard.common.enums.CustomerType;
import io.github.kycguard.generator.model.WatchlistEntry;
import io.github.kycguard.generator.service.WatchlistGenerator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WatchlistGeneratorTest {

    private final WatchlistGenerator generator = new WatchlistGenerator();

    @Test
    @DisplayName("Fixed seed produces completely deterministic output across runs")
    void testDeterminismWithFixedSeed() {
        long seed = 12345L;
        int count = 200;

        List<WatchlistEntry> run1 = generator.generate(count, seed);
        List<WatchlistEntry> run2 = generator.generate(count, seed);

        assertThat(run1).hasSize(count);
        assertThat(run2).hasSize(count);

        for (int i = 0; i < count; i++) {
            WatchlistEntry e1 = run1.get(i);
            WatchlistEntry e2 = run2.get(i);

            assertThat(e1.entryId()).isEqualTo(e2.entryId());
            assertThat(e1.listType()).isEqualTo(e2.listType());
            assertThat(e1.listSource()).isEqualTo(e2.listSource());
            assertThat(e1.primaryName()).isEqualTo(e2.primaryName());
            assertThat(e1.aliases()).containsExactlyElementsOf(e2.aliases());
            assertThat(e1.dateOfBirth()).isEqualTo(e2.dateOfBirth());
            assertThat(e1.countries()).containsExactlyElementsOf(e2.countries());
            assertThat(e1.entityType()).isEqualTo(e2.entityType());
            assertThat(e1.addedAt()).isEqualTo(e2.addedAt());
        }
    }

    @Test
    @DisplayName("Different seeds produce different outputs")
    void testDifferentSeedsProduceDifferentOutputs() {
        List<WatchlistEntry> run1 = generator.generate(50, 111L);
        List<WatchlistEntry> run2 = generator.generate(50, 999L);

        // They should have identical entryId sequences ("WL-000001", etc.) but distinct primary names/types
        List<String> names1 = run1.stream().map(WatchlistEntry::primaryName).toList();
        List<String> names2 = run2.stream().map(WatchlistEntry::primaryName).toList();

        assertThat(names1).isNotEqualTo(names2);
    }

    @Test
    @DisplayName("Watchlist entries satisfy all domain requirements")
    void testWatchlistFieldValidations() {
        List<WatchlistEntry> entries = generator.generate(100, 42L);

        assertThat(entries).hasSize(100);

        for (WatchlistEntry entry : entries) {
            assertThat(entry.entryId()).matches("WL-\\d{6}");
            assertThat(entry.listType()).isNotNull();
            assertThat(entry.listSource()).isNotBlank();
            assertThat(entry.primaryName()).isNotBlank();
            assertThat(entry.countries()).isNotEmpty();
            assertThat(entry.entityType()).isNotNull();
            assertThat(entry.addedAt()).isNotNull();

            if (entry.entityType() == CustomerType.COMPANY) {
                assertThat(entry.dateOfBirth()).isNull();
            } else {
                assertThat(entry.dateOfBirth()).isNotNull();
                assertThat(entry.aliases()).isNotEmpty();
            }
        }
    }
}
