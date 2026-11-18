package io.github.kycguard.customer.service;

import io.github.kycguard.common.enums.CustomerStatus;
import io.github.kycguard.common.enums.RiskBand;
import io.github.kycguard.common.events.CustomerOnboardedV1;
import io.github.kycguard.common.events.EventEnvelope;
import io.github.kycguard.common.util.JsonUtils;
import io.github.kycguard.customer.domain.CustomerAddressEntity;
import io.github.kycguard.customer.domain.CustomerEntity;
import io.github.kycguard.customer.domain.CustomerIdentifierEntity;
import io.github.kycguard.customer.domain.IdempotencyRecordEntity;
import io.github.kycguard.customer.domain.OutboxEventEntity;
import io.github.kycguard.customer.domain.OutboxStatus;
import io.github.kycguard.customer.dto.CreateCustomerRequest;
import io.github.kycguard.customer.dto.CustomerResponse;
import io.github.kycguard.customer.exception.CustomerNotFoundException;
import io.github.kycguard.customer.mapper.CustomerMapper;
import io.github.kycguard.customer.repository.CustomerRepository;
import io.github.kycguard.customer.repository.OutboxEventRepository;
import io.github.kycguard.customer.statemachine.CustomerStateMachine;
import io.github.kycguard.customer.util.CustomerValidator;
import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final OutboxEventRepository outboxRepository;
    private final IdempotencyService idempotencyService;
    private final CustomerStateMachine stateMachine;

    public CustomerService(
            CustomerRepository customerRepository,
            OutboxEventRepository outboxRepository,
            IdempotencyService idempotencyService,
            CustomerStateMachine stateMachine
    ) {
        this.customerRepository = customerRepository;
        this.outboxRepository = outboxRepository;
        this.idempotencyService = idempotencyService;
        this.stateMachine = stateMachine;
    }

    @Transactional
    public CustomerResponse createCustomer(CreateCustomerRequest request, String idempotencyKey, String correlationId) {
        CustomerValidator.validate(request);

        String normalizedName = CustomerValidator.normalizeName(request.fullName());
        CreateCustomerRequest normalizedRequest = new CreateCustomerRequest(
                request.type(),
                normalizedName,
                request.aliases(),
                request.dateOfBirth(),
                request.incorporationDate(),
                request.country().toUpperCase(),
                request.addresses(),
                request.identifiers()
        );

        String requestJson = JsonUtils.toJson(normalizedRequest);
        String requestHash = idempotencyService.computeHash(requestJson);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<IdempotencyRecordEntity> existing = idempotencyService.findExisting(idempotencyKey, requestHash);
            if (existing.isPresent()) {
                return JsonUtils.fromJson(existing.get().getResponseBody(), CustomerResponse.class);
            }
        }

        UUID customerId = UUID.randomUUID();
        Instant now = Instant.now();

        CustomerEntity customer = new CustomerEntity(
                customerId,
                normalizedRequest.type(),
                normalizedName,
                normalizedRequest.aliases() != null ? new HashSet<>(normalizedRequest.aliases()) : new HashSet<>(),
                normalizedRequest.dateOfBirth(),
                normalizedRequest.incorporationDate(),
                normalizedRequest.country(),
                CustomerStatus.PENDING_SCREENING,
                null,
                now,
                now
        );

        if (normalizedRequest.addresses() != null) {
            normalizedRequest.addresses().forEach(a -> customer.addAddress(
                    new CustomerAddressEntity(UUID.randomUUID(), customer, a.street(), a.city(), a.postalCode(), a.country())
            ));
        }

        if (normalizedRequest.identifiers() != null) {
            normalizedRequest.identifiers().forEach(i -> customer.addIdentifier(
                    new CustomerIdentifierEntity(UUID.randomUUID(), customer, i.type(), i.value())
            ));
        }

        CustomerEntity savedCustomer = customerRepository.save(customer);
        CustomerResponse response = CustomerMapper.toResponse(savedCustomer);

        CustomerOnboardedV1 event = CustomerMapper.toEvent(savedCustomer);
        String corrId = (correlationId != null && !correlationId.isBlank()) ? correlationId : UUID.randomUUID().toString();
        EventEnvelope<CustomerOnboardedV1> envelope = EventEnvelope.of(
                CustomerOnboardedV1.EVENT_TYPE,
                CustomerOnboardedV1.SCHEMA_VERSION,
                corrId,
                event
        );

        OutboxEventEntity outboxEvent = new OutboxEventEntity(
                UUID.randomUUID(),
                "Customer",
                customerId.toString(),
                CustomerOnboardedV1.EVENT_TYPE,
                CustomerOnboardedV1.TOPIC,
                JsonUtils.toJson(envelope),
                corrId,
                OutboxStatus.PENDING,
                0,
                now,
                null
        );
        outboxRepository.save(outboxEvent);

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            idempotencyService.record(idempotencyKey, requestHash, 201, JsonUtils.toJson(response));
        }

        return response;
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(UUID id) {
        return customerRepository.findById(id)
                .map(CustomerMapper::toResponse)
                .orElseThrow(() -> new CustomerNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Page<CustomerResponse> getCustomers(CustomerStatus status, RiskBand riskBand, Pageable pageable) {
        return customerRepository.findByStatusAndRiskBand(status, riskBand, pageable)
                .map(CustomerMapper::toResponse);
    }

    @Transactional
    public CustomerResponse updateStatus(UUID id, CustomerStatus targetStatus) {
        CustomerEntity customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        stateMachine.validateTransition(id, customer.getStatus(), targetStatus);
        customer.updateStatus(targetStatus);
        CustomerEntity saved = customerRepository.save(customer);
        return CustomerMapper.toResponse(saved);
    }

    @Transactional
    public CustomerResponse updateRiskBand(UUID id, RiskBand targetRiskBand) {
        CustomerEntity customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(id));

        customer.updateRiskBand(targetRiskBand);
        CustomerEntity saved = customerRepository.save(customer);
        return CustomerMapper.toResponse(saved);
    }
}
