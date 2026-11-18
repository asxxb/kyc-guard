package io.github.kycguard.generator.service;

import io.github.kycguard.common.enums.CustomerStatus;
import io.github.kycguard.common.enums.CustomerType;
import io.github.kycguard.common.enums.IdentifierType;
import io.github.kycguard.common.enums.WatchlistType;
import io.github.kycguard.common.model.AddressDto;
import io.github.kycguard.common.model.IdentifierDto;
import io.github.kycguard.generator.model.SyntheticCustomer;
import io.github.kycguard.generator.model.WatchlistEntry;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

public class CustomerGenerator {

    public List<SyntheticCustomer> generate(int count, double riskyRatio, List<WatchlistEntry> watchlist, long seed) {
        Random random = new Random(seed);
        List<SyntheticCustomer> customers = new ArrayList<>(count);

        int riskyCount = (int) Math.round(count * riskyRatio);
        int normalCount = count - riskyCount;

        // 1. Generate Risky Customers linked to Watchlist Ground Truth
        if (riskyCount > 0) {
            List<WatchlistEntry> effectiveWatchlist = watchlist;
            if (effectiveWatchlist == null || effectiveWatchlist.isEmpty()) {
                effectiveWatchlist = new WatchlistGenerator().generate(Math.max(50, riskyCount * 2), seed);
            }

            for (int i = 0; i < riskyCount; i++) {
                WatchlistEntry target = effectiveWatchlist.get(random.nextInt(effectiveWatchlist.size()));

                String fuzzyName = RandomDataPool.createFuzzyVariant(target.primaryName(), random);
                String country = !target.countries().isEmpty()
                        ? target.countries().get(0)
                        : RandomDataPool.COUNTRIES.get(random.nextInt(RandomDataPool.COUNTRIES.size()));

                LocalDate dob = target.dateOfBirth();
                LocalDate incorp = null;

                if (target.entityType() == CustomerType.COMPANY) {
                    incorp = LocalDate.of(2010 + random.nextInt(12), 1 + random.nextInt(12), 1 + random.nextInt(28));
                } else if (dob == null) {
                    dob = LocalDate.of(1970 + random.nextInt(30), 1 + random.nextInt(12), 1 + random.nextInt(28));
                }

                String riskCategory = target.listType() == WatchlistType.SANCTIONS ? "STRONG_MATCH"
                        : (target.listType() == WatchlistType.PEP ? "PEP_MATCH" : "POSSIBLE_MATCH");

                UUID customerId = new UUID(seed + i, (long) (i + 1) * 31);
                customers.add(new SyntheticCustomer(
                        customerId,
                        target.entityType(),
                        fuzzyName,
                        List.of(target.primaryName()),
                        dob,
                        incorp,
                        country,
                        List.of(createRandomAddress(country, random)),
                        List.of(createRandomIdentifier(target.entityType(), country, random)),
                        CustomerStatus.PENDING_SCREENING,
                        true,
                        target.entryId(),
                        riskCategory
                ));
            }
        }

        // 2. Generate Clean / Normal Customers
        for (int i = 0; i < normalCount; i++) {
            boolean isCompany = random.nextInt(10) < 2;
            CustomerType type = isCompany ? CustomerType.COMPANY : CustomerType.INDIVIDUAL;

            String country = RandomDataPool.COUNTRIES.get(random.nextInt(RandomDataPool.COUNTRIES.size()));
            String name;
            LocalDate dob = null;
            LocalDate incorp = null;

            if (isCompany) {
                String prefix = RandomDataPool.COMPANY_PREFIXES.get(random.nextInt(RandomDataPool.COMPANY_PREFIXES.size()));
                String suffix = RandomDataPool.COMPANY_SUFFIXES.get(random.nextInt(RandomDataPool.COMPANY_SUFFIXES.size()));
                name = prefix + " " + suffix;
                incorp = LocalDate.of(2000 + random.nextInt(23), 1 + random.nextInt(12), 1 + random.nextInt(28));
            } else {
                String first = RandomDataPool.FIRST_NAMES.get(random.nextInt(RandomDataPool.FIRST_NAMES.size()));
                String last = RandomDataPool.LAST_NAMES.get(random.nextInt(RandomDataPool.LAST_NAMES.size()));
                name = first + " " + last;
                dob = LocalDate.of(1960 + random.nextInt(40), 1 + random.nextInt(12), 1 + random.nextInt(28));
            }

            UUID customerId = new UUID(seed + 1000000L + i, (long) (i + 1) * 37);
            customers.add(new SyntheticCustomer(
                    customerId,
                    type,
                    name,
                    List.of(),
                    dob,
                    incorp,
                    country,
                    List.of(createRandomAddress(country, random)),
                    List.of(createRandomIdentifier(type, country, random)),
                    CustomerStatus.PENDING_SCREENING,
                    false,
                    null,
                    "NONE"
            ));
        }

        return customers;
    }

    private AddressDto createRandomAddress(String country, Random random) {
        String street = (10 + random.nextInt(900)) + " " + RandomDataPool.STREETS.get(random.nextInt(RandomDataPool.STREETS.size()));
        String city = RandomDataPool.CITIES.get(random.nextInt(RandomDataPool.CITIES.size()));
        String postal = String.format("%05d", 10000 + random.nextInt(89999));
        return new AddressDto(street, city, postal, country);
    }

    private IdentifierDto createRandomIdentifier(CustomerType type, String country, Random random) {
        if (type == CustomerType.COMPANY) {
            return new IdentifierDto(IdentifierType.REGISTRATION_NO, "REG-" + country + "-" + (100000 + random.nextInt(899999)));
        } else {
            IdentifierType idType = random.nextBoolean() ? IdentifierType.PASSPORT : IdentifierType.NATIONAL_ID;
            return new IdentifierDto(idType, country + "-" + (10000000 + random.nextInt(89999999)));
        }
    }
}
