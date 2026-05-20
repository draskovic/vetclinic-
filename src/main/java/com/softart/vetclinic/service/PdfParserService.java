package com.softart.vetclinic.service;

import com.softart.vetclinic.dto.LabReportParseResult;
import com.softart.vetclinic.entity.Pet;
import com.softart.vetclinic.entity.User;
import com.softart.vetclinic.repository.PetRepository;
import com.softart.vetclinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class PdfParserService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public LabReportParseResult parsePdf(MultipartFile file, UUID clinicId) {
        String text = extractText(file);
        log.info("=== RAW PDF TEXT START ===\n{}\n=== RAW PDF TEXT END ===", text);

        String reportNumber = extractPattern(text, "Broj uzorka:\\s*(\\d+)");
        LocalDate requestedAt = extractDate(text, "Vreme prijema:\\s*(\\d{2}\\.\\d{2}\\.\\d{4})");
        LocalDate completedAt = extractDate(text,
                "Vreme završetka ispitivanja:\\s*(\\d{2}\\.\\d{2}\\.\\d{4})");
        String laboratoryName = extractLaboratoryName(text);
        String analysisType = extractAnalysisType(text);

        // Parsiranje reda: "Broj uzorka: 25101116 Bojana Učajev Gavrilo"
        // Veterinar je između broja uzorka i imena pacijenta (poslednja reč)
        String vetName = null;
        String petName = null;

        Matcher lineMatcher = Pattern.compile(
                "Broj uzorka:\\s*\\d+\\s+(.+)").matcher(text);
        if (lineMatcher.find()) {
            String rest = lineMatcher.group(1).trim();
            // Poslednja reč je ime pacijenta, ostalo je ime veterinara
            int lastSpace = rest.lastIndexOf(' ');
            if (lastSpace > 0) {
                vetName = rest.substring(0, lastSpace).trim();
                petName = rest.substring(lastSpace + 1).trim();
            }
        }

        // Pokušaj pronalaženja pet-a u bazi
        UUID petId = null;
        if (petName != null) {
            List<Pet> pets = petRepository
                    .findByClinicIdAndNameIgnoreCaseAndDeletedFalse(clinicId, petName);
            if (pets.size() == 1) {
                petId = pets.get(0).getId();
            }
        }

        // Pokušaj pronalaženja veterinara u bazi
        UUID vetId = null;
        if (vetName != null) {
            String[] parts = vetName.trim().split("\\s+", 2);
            if (parts.length == 2) {
                List<User> vets = userRepository
                        .findByClinicIdAndFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDeletedFalse(
                                clinicId, parts[0], parts[1]);
                if (vets.size() == 1) {
                    vetId = vets.get(0).getId();
                }
            }
        }

        return new LabReportParseResult(
                reportNumber, petName, petId, vetName, vetId,
                null, null, null, null,
                laboratoryName, analysisType, requestedAt, completedAt
        );
    }

    private String extractText(MultipartFile file) {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (Exception e) {
            log.error("Failed to extract text from PDF", e);
            throw new RuntimeException("Nije moguće pročitati PDF fajl: " + e.getMessage());
        }
    }

    private String extractPattern(String text, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(text);
        return matcher.find() ? matcher.group(1).trim() : null;
    }

    private LocalDate extractDate(String text, String regex) {
        String dateStr = extractPattern(text, regex);
        if (dateStr == null) return null;
        try {
            return LocalDate.parse(dateStr, DATE_FORMAT);
        } catch (Exception e) {
            log.warn("Could not parse date: {}", dateStr);
            return null;
        }
    }

    private String extractLaboratoryName(String text) {
        if (text.contains("smart-lab.rs") || text.contains("smartLAB")) {
            return "smartLAB";
        }
        return null;
    }

    private String extractAnalysisType(String text) {
        // 1. Poznati profili (stara logika)
        if (text.contains("Opšti profil")) return "Opšti profil";
        if (text.contains("Hematološki profil")) return "Hematološki profil";
        if (text.contains("Biohemijski profil")) return "Biohemijski profil";

        boolean hasHem = text.contains("HEMATOLOGIJA");
        boolean hasBio = text.contains("BIOHEMIJA");
        if (hasHem && hasBio) return "Opšti profil";
        if (hasHem) return "Hematologija";
        if (hasBio) return "Biohemija";

        // 2. Poznati tipovi analiza - direktno pretraživanje u tekstu
        String[] knownTypes = {
            "Koprokultura mali antibiogram",
            "Koprokultura veliki antibiogram",
            "Koprokultura",
            "Dijareja profil mali antibiogram",
            "Dijareja profil veliki antibiogram",
            "Dijareja profil",
            "Urinokultura mali antibiogram",
            "Urinokultura veliki antibiogram",
            "Urinokultura",
            "Dermatološki profil",
            "Endokrinološki profil",
            "Koagulacioni profil",
            "Elektroliti",
            "Urin analiza",
            "Citologija",
            "Parazitološki pregled",
            "Brisevi mali antibiogram",
            "Brisevi veliki antibiogram"
        };
        for (String type : knownTypes) {
            if (text.contains(type)) return type;
        }

        // 3. Fallback: tekst između "Vreme početka ispitivanja: dd.MM.yyyy HH:mm" i "Vreme završetka"
        Matcher m = Pattern.compile(
                "Vreme početka ispitivanja:\\s*\\d{2}\\.\\d{2}\\.\\d{4}\\s+\\d{2}:\\d{2}\\s+(.+?)\\s+Vreme završetka"
        ).matcher(text);
        if (m.find()) {
            String candidate = m.group(1).trim();
            // Ukloni ime vlasnika (pretpostavljamo da su prve 2 reči ime i prezime)
            String[] words = candidate.split("\\s+", 3);
            if (words.length >= 3) {
                return words[2]; // sve posle imena i prezimena
            }
            if (!candidate.isEmpty()) {
                return candidate;
            }
        }

        log.warn("Nije moguće prepoznati vrstu analize iz PDF-a");
        return null;
    }

}
