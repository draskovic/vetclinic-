package com.softart.vetclinic.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.softart.vetclinic.dto.ImportDiagnosisRequest;
import com.softart.vetclinic.dto.ImportResultResponse;
import com.softart.vetclinic.dto.ImportResultResponse.ImportError;
import com.softart.vetclinic.entity.Diagnosis;
import com.softart.vetclinic.repository.DiagnosisRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class DiagnosisImportService {

    private final DiagnosisRepository diagnosisRepository;

    public ImportResultResponse importDiagnoses(UUID clinicId, List<ImportDiagnosisRequest> requests) {
        int created = 0;
        int skipped = 0;
        List<ImportError> errors = new ArrayList<>();

        // Nađi poslednji D-code za auto-generisanje
        String maxCode = diagnosisRepository.findMaxCodeByPrefix(clinicId, "D");
        int nextNum = 1;
        if (maxCode != null) {
            try {
                nextNum = Integer.parseInt(maxCode.replaceAll("[^0-9]", "")) + 1;
            } catch (NumberFormatException e) {
                // fallback
            }
        }

        for (ImportDiagnosisRequest req : requests) {
            try {
                // Duplikat zaštita po imenu
                if (diagnosisRepository.existsByClinicIdAndNameAndDeletedFalse(clinicId, req.name().trim())) {
                    skipped++;
                    continue;
                }

                // Odredi code
                String code = req.code();
                if (code == null || code.isBlank()) {
                    code = String.format("D%03d", nextNum);
                    nextNum++;
                }
                // Ako code već postoji, dodeli sledeći slobodan
                while (diagnosisRepository.existsByClinicIdAndCodeAndDeletedFalse(clinicId, code)) {
                    code = String.format("D%03d", nextNum);
                    nextNum++;
                }

                Diagnosis diagnosis = new Diagnosis();
                diagnosis.setClinicId(clinicId);
                diagnosis.setName(req.name().trim());
                diagnosis.setCode(code);
                diagnosis.setCategory(req.category() != null ? req.category().trim() : null);
                diagnosis.setDescription(req.description() != null ? req.description().trim() : null);
                diagnosis.setActive(true);

                diagnosisRepository.save(diagnosis);
                created++;
            } catch (Exception e) {
                log.error("Import greška za dijagnozu {}: {}", req.name(), e.getMessage());
                errors.add(new ImportError(req.code(), req.name(), e.getMessage()));
            }
        }

        log.info("Import dijagnoza završen: {} obrađeno, {} kreirano, {} preskočeno, {} grešaka",
                requests.size(), created, skipped, errors.size());

        return new ImportResultResponse(requests.size(), created, skipped, errors);
    }
}
