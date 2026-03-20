package com.softart.vetclinic.dto;

import java.util.List;

public record ImportResultResponse(
    int totalProcessed,
    int created,
    int skipped,
    List<ImportError> errors
) {
    public record ImportError(
        String clientCode,
        String ownerName,
        String message
    ) {}
}
