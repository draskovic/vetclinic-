package com.softart.vetclinic.controller;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final EntityManager entityManager;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("application", "VetClinic API");
        response.put("status", "UP");
        response.put("timestamp", OffsetDateTime.now().toString());

        try {
            // Test DB konekcije
            Object dbTime = entityManager
                    .createNativeQuery("SELECT NOW()")
                    .getSingleResult();
            response.put("database", "CONNECTED");
            response.put("dbTime", dbTime.toString());

            // Broj tabela
            Object tableCount = entityManager
                    .createNativeQuery("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_type = 'BASE TABLE'")
                    .getSingleResult();
            response.put("tableCount", tableCount);

        } catch (Exception e) {
            response.put("database", "DISCONNECTED");
            response.put("error", e.getMessage());
        }

        return ResponseEntity.ok(response);
    }
}
