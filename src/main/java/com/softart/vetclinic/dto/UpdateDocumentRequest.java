package com.softart.vetclinic.dto;

import com.softart.vetclinic.enums.FileType;

import java.util.UUID;

public record UpdateDocumentRequest(
        UUID petId,
        UUID medicalRecordId,
        UUID uploadedBy,
        String fileName,
        FileType fileType,
        String mimeType,
        Long fileSizeBytes,
        String storagePath,
        String description
) {}
