package com.softart.vetclinic.service;

import com.softart.vetclinic.dto.LabReportParseResult;
import com.softart.vetclinic.entity.Owner;
import com.softart.vetclinic.entity.Pet;
import com.softart.vetclinic.repository.OwnerRepository;
import com.softart.vetclinic.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
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
public class DocxParserService {

    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    public LabReportParseResult parseDocx(MultipartFile file, UUID clinicId) {
        String rawText = extractText(file);
        String text = rawText.replaceAll("\\s+", " ").trim();
        log.info("=== RAW DOCX TEXT START ===\n{}\n=== RAW DOCX TEXT END ===", text);

        String reportNumber = extractReportNumber(text);
        String species = extractPattern(text, "Vrsta\\s*:?\\s*([\\p{L}]+)");
        String petName = extractPattern(text, "Ime\\s*:?\\s*(.+?)\\s+Vlas");
        String ownerName = extractPattern(text, "Vlas\\s*nik\\s*:?\\s*(.+?)\\s+Broj\\s*mi[kc]ro[čc]ipa");
        String microchipNumber = extractMicrochip(text);
        String laboratoryName = (text.contains("Poljoprivredni fakultet") || text.contains("PATOLO"))
                ? "UNS Poljoprivredni fakultet"
                : null;
        String analysisType = extractAnalysisType(text);
        LocalDate completedAt = extractDate(text);

        // Lookup pet po mikročipu prvo (jedinstveni ISO standard), fallback po imenu
        UUID petId = null;
        if (microchipNumber != null && !microchipNumber.isBlank()) {
            List<Pet> pets = petRepository
                    .findByClinicIdAndMicrochipNumberAndDeletedFalse(clinicId, microchipNumber);
            if (pets.size() == 1) petId = pets.get(0).getId();
        }
        if (petId == null && petName != null) {
            List<Pet> pets = petRepository
                    .findByClinicIdAndNameIgnoreCaseAndDeletedFalse(clinicId, petName);
            if (pets.size() == 1) petId = pets.get(0).getId();
        }

        // Lookup owner po imenu i prezimenu
        UUID ownerId = null;
        if (ownerName != null) {
            String[] parts = ownerName.trim().split("\\s+", 2);
            if (parts.length == 2) {
                List<Owner> owners = ownerRepository
                        .findByClinicIdAndFirstNameIgnoreCaseAndLastNameIgnoreCaseAndDeletedFalse(
                                clinicId, parts[0], parts[1]);
                if (owners.size() == 1) ownerId = owners.get(0).getId();
            }
        }

        return new LabReportParseResult(
                reportNumber,
                petName,
                petId,
                null,            // vetName — DOCX format nema vet polje
                null,            // vetId
                ownerName,
                ownerId,
                microchipNumber,
                species,
                laboratoryName,
                analysisType,
                completedAt,     // requestedAt = completedAt (jedan datum u UNS DOCX-u)
                completedAt
        );
    }

    private String extractText(MultipartFile file) {
        try (XWPFDocument doc = new XWPFDocument(file.getInputStream());
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        } catch (Exception e) {
            log.error("Failed to extract text from DOCX", e);
            throw new RuntimeException("Nije moguće pročitati DOCX fajl: " + e.getMessage());
        }
    }

    private String extractPattern(String text, String regex) {
        Matcher m = Pattern.compile(regex, Pattern.UNICODE_CHARACTER_CLASS).matcher(text);
        return m.find() ? m.group(1).trim() : null;
    }

    private String extractReportNumber(String text) {
        // "2 855 D" / "2 732D" / "2 896 D" — varijabilni razmaci, opcioni D sufiks
        Matcher m = Pattern.compile("LABORATORIJSKI\\s+IZVE[ŠS]TAJ\\s+([\\d\\s]+D?)\\b").matcher(text);
        if (!m.find()) return null;
        String raw = m.group(1).trim();
        // Cleanup: ukloni unutrašnje razmake → "2 855 D" → "2855D"
        return raw.replaceAll("\\s+", "");
    }

    private String extractMicrochip(String text) {
        // "688010000074204" ili "6880 38000277351" (sa unutrašnjim razmakom)
        Matcher m = Pattern.compile(
                "Broj\\s*mi[kc]ro[čc]ipa\\s*:?\\s*([\\d\\s]+?)(?=\\s+Po[šs]iljalac|\\s+Hematolog|\\s+Bioh)")
                .matcher(text);
        if (!m.find()) return null;
        return m.group(1).replaceAll("\\s+", "");
    }

    private LocalDate extractDate(String text) {
        // Datum izrade je obično pre "Izrada:" — uzmi najbliži pattern
        Matcher m = Pattern.compile(
                "(\\d{1,2}\\s*\\.\\s*\\d{1,2}\\s*\\.\\s*\\d{4})\\s*\\.?\\s*Izrada")
                .matcher(text);
        if (m.find()) return parseDate(m.group(1));

        // Fallback: poslednji datum u dokumentu
        m = Pattern.compile("(\\d{1,2}\\s*\\.\\s*\\d{1,2}\\s*\\.\\s*\\d{4})").matcher(text);
        String last = null;
        while (m.find()) last = m.group(1);
        return last != null ? parseDate(last) : null;
    }

    private LocalDate parseDate(String dateStr) {
        // Cleanup unutrašnjih razmaka: "23 .1 2 .2025" → "23.12.2025"
        String cleaned = dateStr.replaceAll("\\s+", "");
        try {
            return LocalDate.parse(cleaned, DATE_FORMAT);
        } catch (Exception e) {
            log.warn("Could not parse date: {} (cleaned: {})", dateStr, cleaned);
            return null;
        }
    }

    private String extractAnalysisType(String text) {
        if (text.contains("Opšti profil")) return "Opšti profil";
        if (text.contains("Hematološki profil")) return "Hematološki profil";
        if (text.contains("Biohemijski profil")) return "Biohemijski profil";

        boolean hasHem = text.contains("Hematologija") || text.contains("HEMATOLOGIJA");
        boolean hasBio = text.contains("Biohemija") || text.contains("BIOHEMIJA");
        if (hasHem && hasBio) return "Opšti profil";
        if (hasHem) return "Hematologija";
        if (hasBio) return "Biohemija";

        log.warn("Nije moguće prepoznati vrstu analize iz DOCX-a");
        return null;
    }
}