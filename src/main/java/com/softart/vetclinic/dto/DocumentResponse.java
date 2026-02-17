package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.FileType;

import java.time.OffsetDateTime;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        UUID petId,
        String petName,
        UUID medicalRecordId,
        UUID uploadedBy,
        String uploadedByName,
        String fileName,
        FileType fileType,
        String mimeType,
        Long fileSizeBytes,
        String storagePath,
        String description,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {}
