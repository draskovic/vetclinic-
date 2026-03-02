package com.softart.vetclinic.service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.softart.vetclinic.config.FileStorageConfig;
import com.softart.vetclinic.entity.FileAttachable;
import com.softart.vetclinic.exception.BadRequestException;
import com.softart.vetclinic.exception.ResourceNotFoundException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final FileStorageConfig fileStorageConfig;

    /**
     * Čuva fajl na disk: {baseDir}/{subPath}/{uuid}.pdf
     * Vraća relativnu putanju od baseDir-a.
     */
    public String save(MultipartFile file, String subPath) {
        validateFile(file);

        try {
            Path targetDir = fileStorageConfig.getBasePath().resolve(subPath).normalize();
            Files.createDirectories(targetDir);

            String originalName = file.getOriginalFilename();
            String extension = "";
            if (originalName != null && originalName.contains(".")) {
                extension = originalName.substring(originalName.lastIndexOf("."));
            }
            String uniqueFileName = UUID.randomUUID() + extension;

            Path targetPath = targetDir.resolve(uniqueFileName).normalize();

            if (!targetPath.startsWith(fileStorageConfig.getBasePath())) {
                throw new BadRequestException("Cannot store file outside upload directory");
            }

            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("Saved file: {}", targetPath);

            return fileStorageConfig.getBasePath().relativize(targetPath).toString().replace("\\", "/");
        } catch (IOException e) {
            log.error("Failed to store file: {}", e.getMessage(), e);
            throw new BadRequestException("Failed to store file: " + e.getMessage());
        }
    }

    /**
     * Učitava fajl kao Resource za download.
     */
    public Resource load(String storagePath) {
        try {
            Path filePath = fileStorageConfig.getBasePath().resolve(storagePath).normalize();

            if (!filePath.startsWith(fileStorageConfig.getBasePath())) {
                throw new BadRequestException("Invalid file path");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new ResourceNotFoundException("File", "path", storagePath);
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File", "path", storagePath);
        }
    }

    /**
     * Briše fajl sa diska.
     */
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            Path filePath = fileStorageConfig.getBasePath().resolve(storagePath).normalize();

            if (!filePath.startsWith(fileStorageConfig.getBasePath())) {
                throw new BadRequestException("Invalid file path");
            }

            Files.deleteIfExists(filePath);
            log.info("Deleted file: {}", storagePath);
        } catch (IOException e) {
            log.warn("Failed to delete file {}: {}", storagePath, e.getMessage());
        }
    }

    private void validateFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new BadRequestException("File is empty");
        }
        
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new BadRequestException("File content type is unknown");
        }
        
        List<String> allowedTypes = List.of(
            "application/pdf",
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp",
            "image/tiff"
        );
        
        if (!allowedTypes.contains(contentType)) {
            throw new BadRequestException("File type not allowed: " + contentType 
                + ". Allowed: PDF, JPEG, PNG, GIF, WEBP, BMP, TIFF");
        }
    }

    
    public void attachFile(FileAttachable entity, MultipartFile file, String subPath) {
        // Obriši stari fajl ako postoji
        if (entity.getStoragePath() != null) {
            delete(entity.getStoragePath());
        }
        
        String path = save(file, subPath);
        entity.setFileName(file.getOriginalFilename());
        entity.setMimeType(file.getContentType());
        entity.setFileSizeBytes(file.getSize());
        entity.setStoragePath(path);
    }

    public void detachFile(FileAttachable entity) {
        if (entity.getStoragePath() != null) {
            delete(entity.getStoragePath());
        }
        entity.setFileName(null);
        entity.setMimeType(null);
        entity.setFileSizeBytes(null);
        entity.setStoragePath(null);
    }
}
