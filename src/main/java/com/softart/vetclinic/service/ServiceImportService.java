package com.softart.vetclinic.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.softart.vetclinic.dto.ImportResultResponse;
import com.softart.vetclinic.dto.ImportResultResponse.ImportError;
import com.softart.vetclinic.dto.ImportServiceRequest;
import com.softart.vetclinic.entity.Service;
import com.softart.vetclinic.enums.ServiceCategory;
import com.softart.vetclinic.repository.ServiceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class ServiceImportService {

    private final ServiceRepository serviceRepository;

    
    public ImportResultResponse importServices(UUID clinicId, List<ImportServiceRequest> requests) {
        int created = 0;
        int skipped = 0;
        List<ImportError> errors = new ArrayList<>();

        for (ImportServiceRequest req : requests) {
            try {
                // Duplikat zaštita po SKU
                if (req.sku() != null && !req.sku().isBlank()) {
                    if (serviceRepository.findByClinicIdAndSkuAndDeletedFalse(clinicId, req.sku().trim()).isPresent()) {
                        skipped++;
                        continue;
                    }
                }

             // Duplikat zaštita po imenu
                if (serviceRepository.existsByClinicIdAndNameAndDeletedFalse(clinicId, req.name().trim())) {
                    skipped++;
                    continue;
                }

                Service service = new Service();
                service.setClinicId(clinicId);
                service.setName(req.name().trim());
                service.setSku(req.sku() != null ? req.sku().trim() : null);
                service.setUnit(req.unit() != null ? req.unit().trim() : null);
                service.setPrice(req.price() != null ? req.price() : BigDecimal.ZERO);
                service.setCategory(req.category() != null ? req.category() : ServiceCategory.OTHER);
                service.setTaxRate(new BigDecimal("20.00"));
                service.setActive(true);

                serviceRepository.save(service);
                created++;
            } catch (Exception e) {
                log.error("Import greška za uslugu {} ({}): {}", req.name(), req.sku(), e.getMessage());
                errors.add(new ImportError(req.sku(), req.name(), e.getMessage()));
            }
        }

        log.info("Import usluga završen: {} obrađeno, {} kreirano, {} preskočeno, {} grešaka",
                requests.size(), created, skipped, errors.size());

        return new ImportResultResponse(requests.size(), created, skipped, errors);
    }
}
