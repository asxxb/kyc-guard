package io.github.kycguard.generator.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.kycguard.common.enums.CustomerType;
import io.github.kycguard.common.enums.WatchlistType;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record WatchlistEntry(
        String entryId,
        WatchlistType listType,
        String listSource,
        String primaryName,
        List<String> aliases,
        LocalDate dateOfBirth,
        List<String> countries,
        CustomerType entityType,
        Instant addedAt
) {
    public WatchlistEntry {
        if (aliases == null) {
            aliases = List.of();
        }
        if (countries == null) {
            countries = List.of();
        }
        if (addedAt == null) {
            addedAt = Instant.now();
        }
    }
}
