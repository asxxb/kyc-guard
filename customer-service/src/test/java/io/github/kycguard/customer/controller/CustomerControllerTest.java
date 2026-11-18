package io.github.kycguard.customer.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.kycguard.common.enums.CustomerStatus;
import io.github.kycguard.common.enums.CustomerType;
import io.github.kycguard.common.enums.IdentifierType;
import io.github.kycguard.common.enums.RiskBand;
import io.github.kycguard.common.model.AddressDto;
import io.github.kycguard.common.model.IdentifierDto;
import io.github.kycguard.customer.dto.CreateCustomerRequest;
import io.github.kycguard.customer.dto.CustomerResponse;
import io.github.kycguard.customer.dto.UpdateRiskBandRequest;
import io.github.kycguard.customer.dto.UpdateStatusRequest;
import io.github.kycguard.customer.exception.CustomerNotFoundException;
import io.github.kycguard.customer.exception.IdempotencyConflictException;
import io.github.kycguard.customer.exception.IllegalCustomerStateTransitionException;
import io.github.kycguard.customer.service.CustomerService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CustomerService customerService;

    @Test
    @DisplayName("POST /api/v1/customers with valid payload and Idempotency-Key should return 201 Created")
    void shouldCreateCustomerSuccessfully() throws Exception {
        UUID customerId = UUID.randomUUID();
        CreateCustomerRequest request = new CreateCustomerRequest(
                CustomerType.INDIVIDUAL,
                "Jane Doe",
                List.of("J. Doe"),
                LocalDate.of(1990, 5, 12),
                null,
                "US",
                List.of(new AddressDto("123 Main St", "New York", "10001", "US")),
                List.of(new IdentifierDto(IdentifierType.PASSPORT, "P12345678"))
        );

        CustomerResponse response = new CustomerResponse(
                customerId,
                CustomerType.INDIVIDUAL,
                "Jane Doe",
                List.of("J. Doe"),
                LocalDate.of(1990, 5, 12),
                null,
                "US",
                request.addresses(),
                request.identifiers(),
                CustomerStatus.PENDING_SCREENING,
                null,
                Instant.now(),
                Instant.now()
        );

        when(customerService.createCustomer(any(), eq("key-abc"), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/customers")
                        .header("Idempotency-Key", "key-abc")
                        .header("X-Correlation-Id", "corr-123")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/customers/" + customerId))
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.fullName").value("Jane Doe"))
                .andExpect(jsonPath("$.status").value("PENDING_SCREENING"));
    }

    @Test
    @DisplayName("POST /api/v1/customers without Idempotency-Key header should return 400 Bad Request")
    void shouldReturnBadRequestWhenMissingIdempotencyHeader() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest(
                CustomerType.INDIVIDUAL,
                "Jane Doe",
                List.of(),
                LocalDate.of(1990, 5, 12),
                null,
                "US",
                List.of(),
                List.of(new IdentifierDto(IdentifierType.PASSPORT, "P12345678"))
        );

        mockMvc.perform(post("/api/v1/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Missing Required Header"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/v1/customers with invalid body should return 400 with invalidParams RFC 7807")
    void shouldReturnBadRequestOnValidationFailure() throws Exception {
        CreateCustomerRequest invalidRequest = new CreateCustomerRequest(
                null, // null type
                "",   // blank name
                List.of(),
                null,
                null,
                "INVALID_COUNTRY", // invalid regex
                List.of(),
                List.of() // empty identifiers
        );

        mockMvc.perform(post("/api/v1/customers")
                        .header("Idempotency-Key", "key-xyz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.invalidParams").isArray());
    }

    @Test
    @DisplayName("POST /api/v1/customers with conflicting idempotency key should return 409 Conflict")
    void shouldReturnConflictOnIdempotencyKeyConflict() throws Exception {
        CreateCustomerRequest request = new CreateCustomerRequest(
                CustomerType.INDIVIDUAL,
                "Jane Doe",
                List.of(),
                LocalDate.of(1990, 5, 12),
                null,
                "US",
                List.of(),
                List.of(new IdentifierDto(IdentifierType.PASSPORT, "P12345678"))
        );

        when(customerService.createCustomer(any(), eq("key-conflict"), any()))
                .thenThrow(new IdempotencyConflictException("key-conflict"));

        mockMvc.perform(post("/api/v1/customers")
                        .header("Idempotency-Key", "key-conflict")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Idempotency Conflict"))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    @DisplayName("GET /api/v1/customers/{id} when found returns 200, when not found returns 404")
    void shouldGetCustomer() throws Exception {
        UUID customerId = UUID.randomUUID();
        CustomerResponse response = new CustomerResponse(
                customerId,
                CustomerType.INDIVIDUAL,
                "Jane Doe",
                List.of(),
                LocalDate.of(1990, 5, 12),
                null,
                "US",
                List.of(),
                List.of(),
                CustomerStatus.CLEARED,
                RiskBand.LOW,
                Instant.now(),
                Instant.now()
        );

        when(customerService.getCustomer(customerId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/customers/{id}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId.toString()))
                .andExpect(jsonPath("$.status").value("CLEARED"));

        UUID notFoundId = UUID.randomUUID();
        when(customerService.getCustomer(notFoundId)).thenThrow(new CustomerNotFoundException(notFoundId));

        mockMvc.perform(get("/api/v1/customers/{id}", notFoundId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Customer Not Found"))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    @DisplayName("GET /api/v1/customers with filters returns 200 Page")
    void shouldQueryCustomersWithFilters() throws Exception {
        when(customerService.getCustomers(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of()));

        mockMvc.perform(get("/api/v1/customers")
                        .param("status", "CLEARED")
                        .param("riskBand", "LOW")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("PATCH /api/v1/customers/{id}/status returns updated customer or 409 on illegal transition")
    void shouldUpdateStatus() throws Exception {
        UUID customerId = UUID.randomUUID();
        UpdateStatusRequest request = new UpdateStatusRequest(CustomerStatus.CLEARED);
        CustomerResponse response = new CustomerResponse(
                customerId,
                CustomerType.INDIVIDUAL,
                "Jane Doe",
                List.of(),
                LocalDate.of(1990, 5, 12),
                null,
                "US",
                List.of(),
                List.of(),
                CustomerStatus.CLEARED,
                null,
                Instant.now(),
                Instant.now()
        );

        when(customerService.updateStatus(customerId, CustomerStatus.CLEARED)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/customers/{id}/status", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLEARED"));

        when(customerService.updateStatus(customerId, CustomerStatus.REJECTED))
                .thenThrow(new IllegalCustomerStateTransitionException(customerId, CustomerStatus.CLEARED, CustomerStatus.REJECTED));

        mockMvc.perform(patch("/api/v1/customers/{id}/status", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateStatusRequest(CustomerStatus.REJECTED))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Illegal State Transition"));
    }

    @Test
    @DisplayName("PATCH /api/v1/customers/{id}/risk returns updated customer")
    void shouldUpdateRisk() throws Exception {
        UUID customerId = UUID.randomUUID();
        UpdateRiskBandRequest request = new UpdateRiskBandRequest(RiskBand.HIGH);
        CustomerResponse response = new CustomerResponse(
                customerId,
                CustomerType.INDIVIDUAL,
                "Jane Doe",
                List.of(),
                LocalDate.of(1990, 5, 12),
                null,
                "US",
                List.of(),
                List.of(),
                CustomerStatus.PENDING_SCREENING,
                RiskBand.HIGH,
                Instant.now(),
                Instant.now()
        );

        when(customerService.updateRiskBand(customerId, RiskBand.HIGH)).thenReturn(response);

        mockMvc.perform(patch("/api/v1/customers/{id}/risk", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riskBand").value("HIGH"));
    }
}
