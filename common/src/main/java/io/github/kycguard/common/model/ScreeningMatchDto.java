package io.github.kycguard.common.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.kycguard.common.enums.MatchCategory;
import io.github.kycguard.common.enums.WatchlistType;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScreeningMatchDto(
        String entryId,
        WatchlistType listType,
        String listSource,
        String matchedName,
        MatchCategory matchCategory,
        double score,
        List<String> matchedFields
) {
    public ScreeningMatchDto {
        if (matchedFields == null) {
            matchedFields = List.of();
        }
    }
}
