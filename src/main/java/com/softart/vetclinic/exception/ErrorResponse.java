package com.softart.vetclinic.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        int status,
        String message,
        OffsetDateTime timestamp,
        List<FieldError> errors
) {
    public ErrorResponse(int status, String message) {
        this(status, message, OffsetDateTime.now(), null);
    }

    public ErrorResponse(int status, String message, List<FieldError> errors) {
        this(status, message, OffsetDateTime.now(), errors);
    }

    public record FieldError(String field, String message) {
    }
}
