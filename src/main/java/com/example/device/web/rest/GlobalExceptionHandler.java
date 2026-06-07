package com.example.device.web.rest;

import com.example.device.errors.BusinessRuleViolationException;
import com.example.device.errors.DeviceNotFoundException;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @SuppressWarnings("unused")
    @ExceptionHandler(DeviceNotFoundException.class)
    public ProblemDetail handleDeviceNotFound(DeviceNotFoundException ex) {
        LOG.info("Device not found: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problemDetail.setTitle("Device not found");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(BusinessRuleViolationException.class)
    public ProblemDetail handleBusinessRuleViolation(BusinessRuleViolationException ex) {
        LOG.warn("Business rule violation: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, ex.getMessage());
        problemDetail.setTitle("Business rule violation occurred");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        LOG.warn("Constraint violation: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Constraint violation occurred");
        problemDetail.setTitle("Request validation failed");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ProblemDetail handleOptimisticLockingFailure(ObjectOptimisticLockingFailureException ex) {
        LOG.warn("Optimistic locking failure: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.CONFLICT,
                "The device resource you are trying to update has been modified by another concurrent transaction or retry context."
        );

        problemDetail.setTitle("Concurrent Modification Conflict");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
        LOG.warn("Validation error occurred: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Validation error occurred."
        );
        problemDetail.setTitle("Validation error");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        LOG.warn("Type mismatch error occurred: {}", ex.getMessage());
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Type mismatch error occurred."
        );
        problemDetail.setTitle("Type mismatch error");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleGenericException(HttpMessageNotReadableException ex) {
        LOG.warn("Bad client request.: {}", ex.getMessage(), ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST,
                "Bad client request."
        );
        problemDetail.setTitle("Bad client request");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }

    @SuppressWarnings("unused")
    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleGenericException(RuntimeException ex) {
        LOG.error("An unexpected error occurred: {}", ex.getMessage(), ex);
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected internal server error occurred. Please contact support."
        );
        problemDetail.setTitle("Internal Server Error");
        problemDetail.setProperty("timestamp", Instant.now());

        return problemDetail;
    }
}
