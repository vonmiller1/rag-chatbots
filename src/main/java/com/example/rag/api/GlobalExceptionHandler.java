package com.example.rag.api;

import java.io.IOException;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    private static final String UNEXPECTED_ERROR = "An unexpected error occurred.";

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiError> handleBadRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                exception.getMessage()
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .orElse("Validation failed.");

        return ResponseEntity.badRequest().body(new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                message
        ));
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ApiError> handleIllegalState(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(new ApiError(
                Instant.now(),
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                exception.getMessage()
        ));
    }

    @ExceptionHandler(IOException.class)
    ResponseEntity<ApiError> handleIo(IOException exception) {
        return ResponseEntity.badRequest().body(new ApiError(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Failed to read the uploaded file."
        ));
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> handleUnexpected(Exception exception) {
        log.error("Unhandled request failure", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiError(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                UNEXPECTED_ERROR
        ));
    }

    record ApiError(Instant timestamp, int status, String message) {
    }
}
