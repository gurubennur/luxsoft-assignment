package com.luxsoft.banking.api;

import com.luxsoft.banking.account.AccountNotFoundException;
import com.luxsoft.banking.account.AccountAlreadyExistsException;
import com.luxsoft.banking.api.ApiModels.ErrorResponse;
import jakarta.validation.ConstraintViolationException;

import java.time.Instant;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    /**
     * Converts unknown account errors into HTTP 404 responses.
     */
    @ExceptionHandler(AccountNotFoundException.class)
    ResponseEntity<ErrorResponse> notFound(AccountNotFoundException ex) {
        return response(HttpStatus.NOT_FOUND, "account_not_found", ex.getMessage());
    }

    /**
     * Converts duplicate account numbers and database constraint conflicts into HTTP 409.
     */
    @ExceptionHandler({AccountAlreadyExistsException.class, DataIntegrityViolationException.class})
    ResponseEntity<ErrorResponse> conflict(RuntimeException ex) {
        String message = ex instanceof AccountAlreadyExistsException
                ? ex.getMessage() : "An account number or account constraint conflicts with existing data";
        return response(HttpStatus.CONFLICT, "account_conflict", message);
    }

    /**
     * Converts invalid domain arguments and constraint violations into HTTP 400 responses.
     */
    @ExceptionHandler({IllegalArgumentException.class, ConstraintViolationException.class})
    ResponseEntity<ErrorResponse> badRequest(Exception ex) {
        return response(HttpStatus.BAD_REQUEST, "invalid_request", ex.getMessage());
    }

    /**
     * Returns field-level details for request validation failures.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> validation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));
        return response(HttpStatus.BAD_REQUEST, "validation_failed", message);
    }

    /**
     * Returns a stable message for malformed or missing JSON request bodies.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException ex) {
        return response(HttpStatus.BAD_REQUEST, "invalid_request", "Request body is missing or malformed");
    }

    /**
     * Builds the common timestamped error response.
     */
    private ResponseEntity<ErrorResponse> response(HttpStatus status, String error, String message) {
        return ResponseEntity.status(status).body(new ErrorResponse(error, message, Instant.now()));
    }
}
