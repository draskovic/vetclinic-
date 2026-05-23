package com.softart.vetclinic.controller;

import com.softart.vetclinic.config.security.JwtService;
import com.softart.vetclinic.config.tenant.ClinicContextHolder;
import com.softart.vetclinic.service.DocumentService;

import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/public/documents")
@RequiredArgsConstructor
public class PublicUploadController {

    private final DocumentService documentService;
    private final JwtService jwtService;

    @GetMapping("/token-info")
    public ResponseEntity<Map<String, Object>> getTokenInfo(@RequestParam String token) {
        try {
            Map<String, Object> info = documentService.getUploadTokenInfo(token);
            return ResponseEntity.ok(info);
        } catch (Exception e) {
            log.warn("Nevažeći upload token: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "valid", false,
                    "message", "Token je nevažeći ili je istekao"
            ));
        }
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @RequestParam String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String description) {
        try {
            // Validate + parse token (van tx-a)
            if (!jwtService.isUploadToken(token)) {
                throw new IllegalArgumentException("Nevažeći ili istekao upload token");
            }
            Claims claims = jwtService.parseToken(token);
            UUID petId = UUID.fromString(claims.get("petId", String.class));
            UUID clinicId = UUID.fromString(claims.get("clinicId", String.class));
            UUID uploadedBy = UUID.fromString(claims.get("uploadedBy", String.class));

            ClinicContextHolder.set(clinicId);
            try {
                var document = documentService.uploadFromToken(petId, clinicId, uploadedBy, file, description);
                log.info("Dokument uploadovan putem QR koda: id={}, petId={}",
                        document.getId(), document.getPetId());
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "documentId", document.getId().toString(),
                        "fileName", document.getFileName()
                ));
            } finally {
                ClinicContextHolder.clear();
            }
        } catch (IllegalArgumentException e) {
            log.warn("Upload odbijen — nevažeći token: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Token je nevažeći ili je istekao"
            ));
        } catch (Exception e) {
            log.error("Greška pri uploadu dokumenta: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Greška pri uploadu fajla"
            ));
        }
    }
}
