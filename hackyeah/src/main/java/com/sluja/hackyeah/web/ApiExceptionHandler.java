package com.sluja.hackyeah.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    /**
     * Raised when a handler mixes a validated body with constrained parameters - Spring then runs
     * method validation over the whole signature, so both kinds of error arrive here together.
     */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(HandlerMethodValidationException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        for (ParameterValidationResult result : exception.getParameterValidationResults()) {
            if (result instanceof ParameterErrors bodyErrors) {
                collectFieldErrors(bodyErrors.getFieldErrors(), fieldErrors);
            } else {
                String parameter = result.getMethodParameter().getParameterName();
                result.getResolvableErrors().forEach(error ->
                        fieldErrors.putIfAbsent(parameter == null ? "parameter" : parameter,
                                error.getDefaultMessage()));
            }
        }
        return ResponseEntity.badRequest().body(new ApiError("Validation failed", fieldErrors));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleInvalidBody(MethodArgumentNotValidException exception) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        collectFieldErrors(exception.getBindingResult().getFieldErrors(), fieldErrors);
        return ResponseEntity.badRequest().body(new ApiError("Validation failed", fieldErrors));
    }

    /** Malformed JSON or an unknown enum value - Jackson fails before validation runs. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> handleUnreadableBody(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest().body(ApiError.of("Malformed request body"));
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiError> handleConflict(ConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(NotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError.of(exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiError.of(exception.getMessage()));
    }

    private static void collectFieldErrors(Iterable<FieldError> source, Map<String, String> target) {
        for (FieldError fieldError : source) {
            target.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
    }
}
