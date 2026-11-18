package io.github.kycguard.customer.controller;

import io.github.kycguard.common.error.ErrorResponse;
import io.github.kycguard.customer.exception.CustomerNotFoundException;
import io.github.kycguard.customer.exception.IdempotencyConflictException;
import io.github.kycguard.customer.exception.IllegalCustomerStateTransitionException;
import io.github.kycguard.customer.exception.InvalidCustomerDataException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CustomerNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleCustomerNotFound(CustomerNotFoundException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.of(
                URI.create("https://kycguard.io/errors/customer-not-found"),
                "Customer Not Found",
                HttpStatus.NOT_FOUND.value(),
                ex.getMessage(),
                URI.create(request.getRequestURI())
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorResponse> handleIdempotencyConflict(IdempotencyConflictException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.of(
                URI.create("https://kycguard.io/errors/idempotency-conflict"),
                "Idempotency Conflict",
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                URI.create(request.getRequestURI())
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(IllegalCustomerStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleIllegalStateTransition(IllegalCustomerStateTransitionException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.of(
                URI.create("https://kycguard.io/errors/illegal-state-transition"),
                "Illegal State Transition",
                HttpStatus.CONFLICT.value(),
                ex.getMessage(),
                URI.create(request.getRequestURI())
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(InvalidCustomerDataException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCustomerData(InvalidCustomerDataException ex, HttpServletRequest request) {
        List<ErrorResponse.InvalidParam> params = List.of(
                new ErrorResponse.InvalidParam(ex.getFieldName(), ex.getMessage(), null)
        );
        ErrorResponse error = ErrorResponse.ofValidation(
                URI.create("https://kycguard.io/errors/invalid-customer-data"),
                "Invalid Customer Data",
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                URI.create(request.getRequestURI()),
                params
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<ErrorResponse.InvalidParam> invalidParams = ex.getBindingResult().getFieldErrors().stream()
                .map(f -> new ErrorResponse.InvalidParam(f.getField(), f.getDefaultMessage(), f.getRejectedValue()))
                .toList();

        ErrorResponse error = ErrorResponse.ofValidation(
                URI.create("https://kycguard.io/errors/validation-failed"),
                "Validation Failed",
                HttpStatus.BAD_REQUEST.value(),
                "Request validation failed with " + invalidParams.size() + " errors",
                URI.create(request.getRequestURI()),
                invalidParams
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        ErrorResponse error = ErrorResponse.of(
                URI.create("https://kycguard.io/errors/missing-header"),
                "Missing Required Header",
                HttpStatus.BAD_REQUEST.value(),
                ex.getMessage(),
                URI.create(request.getRequestURI())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled internal error occurred while processing request to {}", request.getRequestURI(), ex);
        ErrorResponse error = ErrorResponse.of(
                URI.create("https://kycguard.io/errors/internal-server-error"),
                "Internal Server Error",
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "An unexpected internal error occurred",
                URI.create(request.getRequestURI())
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
