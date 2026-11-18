package io.github.kycguard.generator;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.kycguard.common.enums.CustomerStatus;
import io.github.kycguard.common.enums.CustomerType;
import io.github.kycguard.generator.model.SyntheticCustomer;
import io.github.kycguard.generator.model.WatchlistEntry;
import io.github.kycguard.generator.service.CustomerGenerator;
import io.github.kycguard.generator.service.WatchlistGenerator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class CustomerGeneratorTest {

    private final CustomerGenerator customerGenerator = new CustomerGenerator();
    private final WatchlistGenerator watchlistGenerator = new WatchlistGenerator();

    @Test
    @DisplayName("Fixed seed produces completely deterministic customer output")
    void testDeterminismWithFixedSeed() {
        long seed = 42L;
        int count = 100;
        double riskyRatio = 0.05;
        List<WatchlistEntry> watchlist = watchlistGenerator.generate(50, seed);

        List<SyntheticCustomer> run1 = customerGenerator.generate(count, riskyRatio, watchlist, seed);
        List<SyntheticCustomer> run2 = customerGenerator.generate(count, riskyRatio, watchlist, seed);

        assertThat(run1).hasSize(count);
        assertThat(run2).hasSize(count);

        for (int i = 0; i < count; i++) {
            SyntheticCustomer c1 = run1.get(i);
            SyntheticCustomer c2 = run2.get(i);

            assertThat(c1.customerId()).isEqualTo(c2.customerId());
            assertThat(c1.type()).isEqualTo(c2.type());
            assertThat(c1.fullName()).isEqualTo(c2.fullName());
            assertThat(c1.aliases()).containsExactlyElementsOf(c2.aliases());
            assertThat(c1.dateOfBirth()).isEqualTo(c2.dateOfBirth());
            assertThat(c1.incorporationDate()).isEqualTo(c2.incorporationDate());
            assertThat(c1.country()).isEqualTo(c2.country());
            assertThat(c1.addresses()).containsExactlyElementsOf(c2.addresses());
            assertThat(c1.identifiers()).containsExactlyElementsOf(c2.identifiers());
            assertThat(c1.status()).isEqualTo(c2.status());
            assertThat(c1.isRisky()).isEqualTo(c2.isRisky());
            assertThat(c1.groundTruthWatchlistId()).isEqualTo(c2.groundTruthWatchlistId());
            assertThat(c1.groundTruthRiskCategory()).isEqualTo(c2.groundTruthRiskCategory());
        }
    }

    @Test
    @DisplayName("Different seeds produce different customer outputs")
    void testDifferentSeedsProduceDifferentCustomers() {
        List<WatchlistEntry> watchlist = watchlistGenerator.generate(50, 100L);
        List<SyntheticCustomer> run1 = customerGenerator.generate(50, 0.1, watchlist, 100L);
        List<SyntheticCustomer> run2 = customerGenerator.generate(50, 0.1, watchlist, 200L);

        List<String> names1 = run1.stream().map(SyntheticCustomer::fullName).toList();
        List<String> names2 = run2.stream().map(SyntheticCustomer::fullName).toList();

        assertThat(names1).isNotEqualTo(names2);
    }

    @Test
    @DisplayName("Risky customers exactly match the requested ratio and ground truth entries")
    void testGroundTruthLabelingAndRatio() {
        int count = 500;
        double riskyRatio = 0.06;
        int expectedRisky = (int) Math.round(count * riskyRatio); // 30
        int expectedClean = count - expectedRisky; // 470

        List<WatchlistEntry> watchlist = watchlistGenerator.generate(100, 777L);
        Set<String> watchlistIds = watchlist.stream().map(WatchlistEntry::entryId).collect(Collectors.toSet());

        List<SyntheticCustomer> customers = customerGenerator.generate(count, riskyRatio, watchlist, 777L);

        assertThat(customers).hasSize(count);

        List<SyntheticCustomer> riskyCustomers = customers.stream().filter(SyntheticCustomer::isRisky).toList();
        List<SyntheticCustomer> cleanCustomers = customers.stream().filter(c -> !c.isRisky()).toList();

        assertThat(riskyCustomers).hasSize(expectedRisky);
        assertThat(cleanCustomers).hasSize(expectedClean);

        for (SyntheticCustomer risky : riskyCustomers) {
            assertThat(risky.groundTruthWatchlistId()).isNotNull();
            assertThat(watchlistIds).contains(risky.groundTruthWatchlistId());
            assertThat(risky.groundTruthRiskCategory())
                    .isIn("STRONG_MATCH", "PEP_MATCH", "POSSIBLE_MATCH");
            assertThat(risky.aliases()).isNotEmpty();
        }

        for (SyntheticCustomer clean : cleanCustomers) {
            assertThat(clean.groundTruthWatchlistId()).isNull();
            assertThat(clean.groundTruthRiskCategory()).isEqualTo("NONE");
        }
    }

    @Test
    @DisplayName("All customers contain valid addresses, identifiers, and status")
    void testCustomerFieldsAndCompleteness() {
        List<SyntheticCustomer> customers = customerGenerator.generate(100, 0.05, null, 888L);

        assertThat(customers).hasSize(100);

        for (SyntheticCustomer c : customers) {
            assertThat(c.customerId()).isNotNull();
            assertThat(c.fullName()).isNotBlank();
            assertThat(c.country()).hasSize(2);
            assertThat(c.status()).isEqualTo(CustomerStatus.PENDING_SCREENING);

            assertThat(c.addresses()).isNotEmpty();
            c.addresses().forEach(a -> {
                assertThat(a.street()).isNotBlank();
                assertThat(a.city()).isNotBlank();
                assertThat(a.postalCode()).isNotBlank();
                assertThat(a.country()).isNotBlank();
            });

            assertThat(c.identifiers()).isNotEmpty();
            c.identifiers().forEach(id -> {
                assertThat(id.type()).isNotNull();
                assertThat(id.value()).isNotBlank();
            });

            if (c.type() == CustomerType.COMPANY) {
                assertThat(c.incorporationDate()).isNotNull();
            } else {
                assertThat(c.dateOfBirth()).isNotNull();
            }
        }
    }

    @Test
    @DisplayName("Fallback watchlist is used when watchlist is null or empty")
    void testFallbackWatchlist() {
        List<SyntheticCustomer> customersWithNull = customerGenerator.generate(50, 0.1, null, 999L);
        assertThat(customersWithNull).hasSize(50);
        long riskyCountNull = customersWithNull.stream().filter(SyntheticCustomer::isRisky).count();
        assertThat(riskyCountNull).isEqualTo(5);

        List<SyntheticCustomer> customersWithEmpty = customerGenerator.generate(50, 0.1, List.of(), 999L);
        assertThat(customersWithEmpty).hasSize(50);
        long riskyCountEmpty = customersWithEmpty.stream().filter(SyntheticCustomer::isRisky).count();
        assertThat(riskyCountEmpty).isEqualTo(5);
    }
}
