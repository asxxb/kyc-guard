package io.github.kycguard.generator.service;

import io.github.kycguard.common.enums.CustomerType;
import io.github.kycguard.common.enums.WatchlistType;
import io.github.kycguard.generator.model.WatchlistEntry;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WatchlistGenerator {

    public List<WatchlistEntry> generate(int count, long seed) {
        Random random = new Random(seed);
        List<WatchlistEntry> entries = new ArrayList<>(count);

        Instant baseTime = Instant.parse("2024-01-01T00:00:00Z");

        for (int i = 1; i <= count; i++) {
            String entryId = String.format("WL-%06d", i);

            // 75% Sanctions, 20% PEP, 5% Adverse Media
            int typeRoll = random.nextInt(100);
            WatchlistType listType = typeRoll < 75 ? WatchlistType.SANCTIONS
                    : (typeRoll < 95 ? WatchlistType.PEP : WatchlistType.ADVERSE_MEDIA);

            String listSource = RandomDataPool.SOURCES.get(random.nextInt(RandomDataPool.SOURCES.size()));
            boolean isCompany = random.nextInt(10) < 2; // 20% companies
            CustomerType entityType = isCompany ? CustomerType.COMPANY : CustomerType.INDIVIDUAL;

            String primaryName;
            List<String> aliases = new ArrayList<>();
            LocalDate dob = null;

            if (isCompany) {
                String prefix = RandomDataPool.COMPANY_PREFIXES.get(random.nextInt(RandomDataPool.COMPANY_PREFIXES.size()));
                String suffix = RandomDataPool.COMPANY_SUFFIXES.get(random.nextInt(RandomDataPool.COMPANY_SUFFIXES.size()));
                primaryName = prefix + " " + suffix;
                if (random.nextBoolean()) {
                    aliases.add(prefix + " Trading");
                }
            } else {
                String first = RandomDataPool.FIRST_NAMES.get(random.nextInt(RandomDataPool.FIRST_NAMES.size()));
                String last = RandomDataPool.LAST_NAMES.get(random.nextInt(RandomDataPool.LAST_NAMES.size()));
                primaryName = first + " " + last;

                // Add transliteration / alias variants
                aliases.add(last + ", " + first);
                if (RandomDataPool.TRANSLITERATIONS.containsKey(first)) {
                    aliases.add(RandomDataPool.TRANSLITERATIONS.get(first) + " " + last);
                }

                int birthYear = 1950 + random.nextInt(50);
                int birthMonth = 1 + random.nextInt(12);
                int birthDay = 1 + random.nextInt(28);
                dob = LocalDate.of(birthYear, birthMonth, birthDay);
            }

            int countryCount = 1 + random.nextInt(2);
            List<String> countries = new ArrayList<>();
            for (int c = 0; c < countryCount; c++) {
                String country = RandomDataPool.COUNTRIES.get(random.nextInt(RandomDataPool.COUNTRIES.size()));
                if (!countries.contains(country)) {
                    countries.add(country);
                }
            }

            Instant addedAt = baseTime.plusSeconds((long) i * 3600);

            entries.add(new WatchlistEntry(
                    entryId,
                    listType,
                    listSource,
                    primaryName,
                    aliases,
                    dob,
                    countries,
                    entityType,
                    addedAt
            ));
        }

        return entries;
    }
}
