package io.github.kycguard.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.net.URI;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        URI type,
        String title,
        int status,
        String detail,
        URI instance,
        Instant timestamp,
        List<InvalidParam> invalidParams
) {
    public ErrorResponse {
        if (timestamp == null) {
            timestamp = Instant.now();
        }
    }

    public static ErrorResponse of(URI type, String title, int status, String detail, URI instance) {
        return new ErrorResponse(type, title, status, detail, instance, Instant.now(), null);
    }

    public static ErrorResponse ofValidation(URI type, String title, int status, String detail, URI instance, List<InvalidParam> invalidParams) {
        return new ErrorResponse(type, title, status, detail, instance, Instant.now(), invalidParams);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record InvalidParam(
            String name,
            String reason,
            Object rejectedValue
    ) {
    }
}
