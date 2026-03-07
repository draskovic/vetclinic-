package com.softart.vetclinic.controller;

import com.softart.vetclinic.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/public/documents")
@RequiredArgsConstructor
public class PublicUploadController {

    private final DocumentService documentService;

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
            var document = documentService.uploadFromToken(token, file, description);
            log.info("Dokument uploadovan putem QR koda: id={}, petId={}", 
                    document.getId(), document.getPetId());
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "documentId", document.getId().toString(),
                    "fileName", document.getFileName()
            ));
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
