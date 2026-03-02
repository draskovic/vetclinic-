package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.FileType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

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
