package io.github.kycguard.customer.controller;

import io.github.kycguard.common.enums.CustomerStatus;
import io.github.kycguard.common.enums.RiskBand;
import io.github.kycguard.common.error.ErrorResponse;
import io.github.kycguard.customer.dto.CreateCustomerRequest;
import io.github.kycguard.customer.dto.CustomerResponse;
import io.github.kycguard.customer.dto.UpdateRiskBandRequest;
import io.github.kycguard.customer.dto.UpdateStatusRequest;
import io.github.kycguard.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Management", description = "Customer onboarding, queries, and status management")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping(consumes = "application/json", produces = "application/json")
    @Operation(summary = "Onboard a new customer", description = "Creates a customer profile, validates inputs, records idempotency and queues customer.onboarded event")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Customer onboarded successfully",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request payload or missing header",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Idempotency key reused with conflicting payload",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CustomerResponse> createCustomer(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-Id", required = false) String correlationId,
            @Valid @RequestBody CreateCustomerRequest request
    ) {
        CustomerResponse response = customerService.createCustomer(request, idempotencyKey, correlationId);
        URI location = URI.create("/api/v1/customers/" + response.id());
        return ResponseEntity.created(location).body(response);
    }

    @GetMapping(value = "/{id}", produces = "application/json")
    @Operation(summary = "Get customer by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Customer details retrieved"),
            @ApiResponse(responseCode = "404", description = "Customer not found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable("id") UUID id) {
        CustomerResponse response = customerService.getCustomer(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping(produces = "application/json")
    @Operation(summary = "Search and filter customers")
    public ResponseEntity<Page<CustomerResponse>> getCustomers(
            @RequestParam(value = "status", required = false) CustomerStatus status,
            @RequestParam(value = "riskBand", required = false) RiskBand riskBand,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CustomerResponse> page = customerService.getCustomers(status, riskBand, pageable);
        return ResponseEntity.ok(page);
    }

    @PatchMapping(value = "/{id}/status", consumes = "application/json", produces = "application/json")
    @Operation(summary = "Update customer screening/review status")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Status updated"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "409", description = "Illegal state transition")
    })
    public ResponseEntity<CustomerResponse> updateStatus(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateStatusRequest request
    ) {
        CustomerResponse response = customerService.updateStatus(id, request.status());
        return ResponseEntity.ok(response);
    }

    @PatchMapping(value = "/{id}/risk", consumes = "application/json", produces = "application/json")
    @Operation(summary = "Update customer risk band")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Risk band updated"),
            @ApiResponse(responseCode = "404", description = "Customer not found")
    })
    public ResponseEntity<CustomerResponse> updateRiskBand(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateRiskBandRequest request
    ) {
        CustomerResponse response = customerService.updateRiskBand(id, request.riskBand());
        return ResponseEntity.ok(response);
    }
}
