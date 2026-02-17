package com.softart.vetclinic.service;

import com.softart.vetclinic.config.FileStorageConfig;
import com.softart.vetclinic.exception.BadRequestException;
import com.softart.vetclinic.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

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
        if (contentType == null || !contentType.equals("application/pdf")) {
            throw new BadRequestException("Only PDF files are allowed. Got: " + contentType);
        }
    }
}
