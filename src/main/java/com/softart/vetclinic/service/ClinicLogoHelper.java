package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Clinic;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@RequiredArgsConstructor
@Slf4j
public class ClinicLogoHelper {

    private final FileStorageService fileStorageService;

    /**
     * Čita logo klinike sa diska i vraća kao Base64 Data URI
     * za ugradnju u PDF preko Thymeleaf/Flying Saucer <img>.
     * Vraća null ako klinika nema logo ili ako fajl nije dostupan.
     */
    public String toDataUri(Clinic clinic) {
        if (clinic == null || clinic.getLogoUrl() == null || clinic.getLogoUrl().isBlank()) {
            return null;
        }
        try {
            Resource resource = fileStorageService.load(clinic.getLogoUrl());
            byte[] bytes = resource.getInputStream().readAllBytes();
            String base64 = Base64.getEncoder().encodeToString(bytes);

            String mimeType = detectMimeType(clinic.getLogoUrl());
            return "data:" + mimeType + ";base64," + base64;
        } catch (Exception e) {
            log.warn("Nije moguće učitati logo klinike {}: {}", clinic.getId(), e.getMessage());
            return null;
        }
    }

    private String detectMimeType(String path) {
        String lower = path.toLowerCase();
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".gif")) return "image/gif";
        if (lower.endsWith(".webp")) return "image/webp";
        if (lower.endsWith(".bmp")) return "image/bmp";
        return "image/png";
    }
}
