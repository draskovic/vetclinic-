package com.softart.vetclinic.dto;

import java.util.UUID;

import com.softart.vetclinic.enums.FileType;

import jakarta.validation.constraints.NotNull;

public record CreateDocumentRequest(
        UUID petId,
        UUID medicalRecordId,
        @NotNull UUID uploadedBy,
        String fileName,
        @NotNull FileType fileType,
        String mimeType,
        Long fileSizeBytes,
        String storagePath,
        String description
) {}
