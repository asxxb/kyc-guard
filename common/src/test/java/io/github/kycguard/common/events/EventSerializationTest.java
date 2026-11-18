package io.github.kycguard.common.events;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.core.type.TypeReference;
import io.github.kycguard.common.enums.AlertSeverity;
import io.github.kycguard.common.enums.CustomerStatus;
import io.github.kycguard.common.enums.CustomerType;
import io.github.kycguard.common.enums.IdentifierType;
import io.github.kycguard.common.enums.MatchCategory;
import io.github.kycguard.common.enums.RiskBand;
import io.github.kycguard.common.enums.TransactionType;
import io.github.kycguard.common.enums.WatchlistType;
import io.github.kycguard.common.model.AddressDto;
import io.github.kycguard.common.model.BeneficialOwnerDto;
import io.github.kycguard.common.model.IdentifierDto;
import io.github.kycguard.common.model.RiskReasonDto;
import io.github.kycguard.common.model.ScreeningMatchDto;
import io.github.kycguard.common.util.JsonUtils;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class EventSerializationTest {

    @Test
    @DisplayName("Should serialize and deserialize CustomerOnboardedV1 within EventEnvelope")
    void shouldSerializeAndDeserializeCustomerOnboarded() {
        UUID customerId = UUID.randomUUID();
        CustomerOnboardedV1 payload = new CustomerOnboardedV1(
                customerId,
                CustomerType.INDIVIDUAL,
                "Alex Rivera",
                List.of("Alexander Rivera"),
                LocalDate.of(1985, 6, 15),
                null,
                "US",
                List.of(new AddressDto("123 Main St", "New York", "10001", "US")),
                List.of(new IdentifierDto(IdentifierType.PASSPORT, "A12345678")),
                CustomerStatus.PENDING_SCREENING,
                Instant.parse("2026-11-18T12:00:00Z")
        );

        EventEnvelope<CustomerOnboardedV1> envelope = EventEnvelope.of(
                CustomerOnboardedV1.EVENT_TYPE,
                CustomerOnboardedV1.SCHEMA_VERSION,
                "corr-123",
                payload
        );

        String json = JsonUtils.toJson(envelope);
        assertThat(json)
                .contains("CustomerOnboarded")
                .contains("Alex Rivera")
                .contains("corr-123")
                .contains("PENDING_SCREENING")
                .contains("A12345678");

        EventEnvelope<CustomerOnboardedV1> deserialized = JsonUtils.fromJson(
                json,
                new TypeReference<EventEnvelope<CustomerOnboardedV1>>() {}
        );

        assertThat(deserialized.eventId()).isEqualTo(envelope.eventId());
        assertThat(deserialized.eventType()).isEqualTo(CustomerOnboardedV1.EVENT_TYPE);
        assertThat(deserialized.schemaVersion()).isEqualTo(1);
        assertThat(deserialized.correlationId()).isEqualTo("corr-123");
        assertThat(deserialized.payload().customerId()).isEqualTo(customerId);
        assertThat(deserialized.payload().fullName()).isEqualTo("Alex Rivera");
        assertThat(deserialized.payload().customerType()).isEqualTo(CustomerType.INDIVIDUAL);
        assertThat(deserialized.payload().identifiers()).hasSize(1);
        assertThat(deserialized.payload().identifiers().getFirst().value()).isEqualTo("A12345678");
    }

    @Test
    @DisplayName("Should serialize and deserialize ScreeningCompletedV1 within EventEnvelope")
    void shouldSerializeAndDeserializeScreeningCompleted() {
        UUID customerId = UUID.randomUUID();
        ScreeningMatchDto match = new ScreeningMatchDto(
                "WL-9901",
                WatchlistType.SANCTIONS,
                "OFAC-SDN",
                "Alexander Rivera",
                MatchCategory.STRONG,
                0.94,
                List.of("primaryName", "dateOfBirth")
        );

        ScreeningCompletedV1 payload = new ScreeningCompletedV1(
                customerId,
                "Alex Rivera",
                List.of(match),
                Instant.parse("2026-11-18T12:01:00Z")
        );

        EventEnvelope<ScreeningCompletedV1> envelope = EventEnvelope.of(
                ScreeningCompletedV1.EVENT_TYPE,
                ScreeningCompletedV1.SCHEMA_VERSION,
                "corr-screen-456",
                payload
        );

        String json = JsonUtils.toJson(envelope);
        assertThat(json)
                .contains("ScreeningCompleted")
                .contains("OFAC-SDN")
                .contains("STRONG")
                .contains("0.94");

        EventEnvelope<ScreeningCompletedV1> deserialized = JsonUtils.fromJson(
                json,
                new TypeReference<EventEnvelope<ScreeningCompletedV1>>() {}
        );

        assertThat(deserialized.payload().customerId()).isEqualTo(customerId);
        assertThat(deserialized.payload().matches()).hasSize(1);
        assertThat(deserialized.payload().matches().getFirst().score()).isEqualTo(0.94);
        assertThat(deserialized.payload().matches().getFirst().matchCategory()).isEqualTo(MatchCategory.STRONG);
    }

    @Test
    @DisplayName("Should serialize and deserialize OwnershipUpdatedV1 within EventEnvelope")
    void shouldSerializeAndDeserializeOwnershipUpdated() {
        UUID customerId = UUID.randomUUID();
        UUID entityId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        BeneficialOwnerDto owner = new BeneficialOwnerDto(
                ownerId,
                "Jane Doe",
                "PERSON",
                new BigDecimal("65.5000"),
                2,
                false,
                RiskBand.LOW
        );

        OwnershipUpdatedV1 payload = new OwnershipUpdatedV1(
                customerId,
                entityId,
                "Acme Global Holdings Ltd",
                List.of(owner),
                3,
                Instant.parse("2026-11-18T12:02:00Z")
        );

        EventEnvelope<OwnershipUpdatedV1> envelope = EventEnvelope.of(
                OwnershipUpdatedV1.EVENT_TYPE,
                OwnershipUpdatedV1.SCHEMA_VERSION,
                "corr-owner-789",
                payload
        );

        String json = JsonUtils.toJson(envelope);
        assertThat(json)
                .contains("OwnershipUpdated")
                .contains("Acme Global Holdings Ltd")
                .contains("Jane Doe")
                .contains("65.5000");

        EventEnvelope<OwnershipUpdatedV1> deserialized = JsonUtils.fromJson(
                json,
                new TypeReference<EventEnvelope<OwnershipUpdatedV1>>() {}
        );

        assertThat(deserialized.payload().customerId()).isEqualTo(customerId);
        assertThat(deserialized.payload().maxOwnershipDepth()).isEqualTo(3);
        assertThat(deserialized.payload().ultimateBeneficialOwners()).hasSize(1);
        assertThat(deserialized.payload().ultimateBeneficialOwners().getFirst().ownershipPercentage())
                .isEqualTo(new BigDecimal("65.5000"));
    }

    @Test
    @DisplayName("Should serialize and deserialize RiskAssessedV1 within EventEnvelope")
    void shouldSerializeAndDeserializeRiskAssessed() {
        UUID customerId = UUID.randomUUID();
        RiskReasonDto reason1 = new RiskReasonDto("Strong sanctions match", 60, "OFAC-SDN entry WL-9901");
        RiskReasonDto reason2 = new RiskReasonDto("High-risk jurisdiction", 15, "Nationality: IR");

        RiskAssessedV1 payload = new RiskAssessedV1(
                customerId,
                75,
                RiskBand.HIGH,
                "scorecard-v1.0",
                List.of(reason1, reason2),
                Instant.parse("2026-11-18T12:03:00Z")
        );

        EventEnvelope<RiskAssessedV1> envelope = EventEnvelope.of(
                RiskAssessedV1.EVENT_TYPE,
                RiskAssessedV1.SCHEMA_VERSION,
                "corr-risk-101",
                payload
        );

        String json = JsonUtils.toJson(envelope);
        assertThat(json)
                .contains("RiskAssessed")
                .contains("HIGH")
                .contains("scorecard-v1.0")
                .contains("Strong sanctions match")
                .contains("OFAC-SDN entry WL-9901");

        EventEnvelope<RiskAssessedV1> deserialized = JsonUtils.fromJson(
                json,
                new TypeReference<EventEnvelope<RiskAssessedV1>>() {}
        );

        assertThat(deserialized.payload().customerId()).isEqualTo(customerId);
        assertThat(deserialized.payload().score()).isEqualTo(75);
        assertThat(deserialized.payload().band()).isEqualTo(RiskBand.HIGH);
        assertThat(deserialized.payload().reasons()).hasSize(2);
        assertThat(deserialized.payload().reasons().getFirst().points()).isEqualTo(60);
    }

    @Test
    @DisplayName("Should serialize and deserialize TransactionV1 within EventEnvelope")
    void shouldSerializeAndDeserializeTransaction() {
        UUID txnId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        TransactionV1 payload = new TransactionV1(
                txnId,
                "ACC-12345",
                customerId,
                new BigDecimal("9500.0000"),
                "USD",
                TransactionType.DEPOSIT,
                "ACC-BRANCH-CASH",
                "US",
                "BRANCH",
                Instant.parse("2026-11-18T12:04:00Z")
        );

        EventEnvelope<TransactionV1> envelope = EventEnvelope.of(
                TransactionV1.EVENT_TYPE,
                TransactionV1.SCHEMA_VERSION,
                "corr-txn-202",
                payload
        );

        String json = JsonUtils.toJson(envelope);
        assertThat(json)
                .contains("Transaction")
                .contains("ACC-12345")
                .contains("9500.0000")
                .contains("DEPOSIT");

        EventEnvelope<TransactionV1> deserialized = JsonUtils.fromJson(
                json,
                new TypeReference<EventEnvelope<TransactionV1>>() {}
        );

        assertThat(deserialized.payload().txnId()).isEqualTo(txnId);
        assertThat(deserialized.payload().amount()).isEqualTo(new BigDecimal("9500.0000"));
        assertThat(deserialized.payload().currency()).isEqualTo("USD");
        assertThat(deserialized.payload().type()).isEqualTo(TransactionType.DEPOSIT);
    }

    @Test
    @DisplayName("Should serialize and deserialize AlertRaisedV1 within EventEnvelope")
    void shouldSerializeAndDeserializeAlertRaised() {
        UUID alertId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID txn1 = UUID.randomUUID();
        UUID txn2 = UUID.randomUUID();
        UUID txn3 = UUID.randomUUID();

        AlertRaisedV1 payload = new AlertRaisedV1(
                alertId,
                customerId,
                "AML-RULE-001-STRUCTURING",
                AlertSeverity.HIGH,
                Instant.parse("2026-11-17T12:00:00Z"),
                Instant.parse("2026-11-18T12:00:00Z"),
                List.of(txn1, txn2, txn3),
                "3 cash deposits between 80% and 99% of 10,000 threshold within 24h",
                Instant.parse("2026-11-18T12:05:00Z")
        );

        EventEnvelope<AlertRaisedV1> envelope = EventEnvelope.of(
                AlertRaisedV1.EVENT_TYPE,
                AlertRaisedV1.SCHEMA_VERSION,
                "corr-alert-303",
                payload
        );

        String json = JsonUtils.toJson(envelope);
        assertThat(json)
                .contains("AlertRaised")
                .contains("AML-RULE-001-STRUCTURING")
                .contains("HIGH")
                .contains("3 cash deposits");

        EventEnvelope<AlertRaisedV1> deserialized = JsonUtils.fromJson(
                json,
                new TypeReference<EventEnvelope<AlertRaisedV1>>() {}
        );

        assertThat(deserialized.payload().alertId()).isEqualTo(alertId);
        assertThat(deserialized.payload().severity()).isEqualTo(AlertSeverity.HIGH);
        assertThat(deserialized.payload().txnIds()).hasSize(3);
    }

    @Test
    @DisplayName("Should apply envelope defaults when optional parameters are omitted")
    void shouldApplyEnvelopeDefaults() {
        UUID customerId = UUID.randomUUID();
        CustomerOnboardedV1 payload = new CustomerOnboardedV1(
                customerId,
                CustomerType.COMPANY,
                "Apex Trade Inc",
                null,
                null,
                LocalDate.of(2010, 5, 20),
                "GB",
                null,
                null,
                CustomerStatus.PENDING_SCREENING,
                null
        );

        EventEnvelope<CustomerOnboardedV1> envelope = EventEnvelope.of(
                null,
                "CustomEvent",
                0,
                null,
                null,
                payload
        );

        assertThat(envelope.eventId()).isNotNull();
        assertThat(envelope.occurredAt()).isNotNull();
        assertThat(envelope.correlationId()).isNotNull();
        assertThat(envelope.schemaVersion()).isEqualTo(1);
        assertThat(envelope.payload().aliases()).isEmpty();
        assertThat(envelope.payload().addresses()).isEmpty();
        assertThat(envelope.payload().identifiers()).isEmpty();
    }

    @Test
    @DisplayName("Should enforce non-null requirements on envelope creation")
    void shouldEnforceNonNullRequirements() {
        assertThatThrownBy(() -> new EventEnvelope<>(UUID.randomUUID(), null, 1, Instant.now(), "corr", "payload"))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("eventType cannot be null");

        assertThatThrownBy(() -> new EventEnvelope<>(UUID.randomUUID(), "type", 1, Instant.now(), "corr", null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("payload cannot be null");
    }
}
