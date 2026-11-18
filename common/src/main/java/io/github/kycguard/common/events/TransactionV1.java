package io.github.kycguard.common.events;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.kycguard.common.enums.TransactionType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TransactionV1(
        UUID txnId,
        String accountId,
        UUID customerId,
        BigDecimal amount,
        String currency,
        TransactionType type,
        String counterpartyAccountId,
        String counterpartyCountry,
        String channel,
        Instant occurredAt
) {
    public static final String TOPIC = "transactions.raw";
    public static final String EVENT_TYPE = "Transaction";
    public static final int SCHEMA_VERSION = 1;

    public TransactionV1 {
        if (txnId == null) {
            txnId = UUID.randomUUID();
        }
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }
}
