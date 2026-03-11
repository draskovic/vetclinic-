package com.softart.vetclinic.service;

import com.lowagie.text.pdf.BaseFont;
import com.softart.vetclinic.entity.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;
import com.softart.vetclinic.repository.ClinicRepository;
import com.softart.vetclinic.exception.ResourceNotFoundException;

import com.softart.vetclinic.repository.OwnerRepository;
import com.softart.vetclinic.repository.SpeciesRepository;
import com.softart.vetclinic.repository.BreedRepository;
import com.softart.vetclinic.entity.Treatment;
import com.softart.vetclinic.entity.LabReport;


import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.UUID;

@Service
public class MedicalRecordPdfService {

    private static final Logger log = LoggerFactory.getLogger(MedicalRecordPdfService.class);

    private final MedicalRecordService medicalRecordService;
    private final PrescriptionService prescriptionService;
    private final VaccinationService vaccinationService;
    private final ClinicRepository clinicRepository;
    private final TemplateEngine templateEngine;
    private final TreatmentService treatmentService;
    private final LabReportService labReportService;

    
    
    private final OwnerRepository ownerRepository;
    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;


    public MedicalRecordPdfService(MedicalRecordService medicalRecordService,
                                    PrescriptionService prescriptionService,
                                    VaccinationService vaccinationService,
                                    ClinicRepository clinicRepository,
                                    TemplateEngine templateEngine,
                                    OwnerRepository ownerRepository,
                                    SpeciesRepository speciesRepository,
                                    BreedRepository breedRepository,
                                    TreatmentService treatmentService,
                                    LabReportService labReportService) {
        this.medicalRecordService = medicalRecordService;
        this.prescriptionService = prescriptionService;
        this.vaccinationService = vaccinationService;
        this.clinicRepository = clinicRepository;
        this.templateEngine = templateEngine;
        this.ownerRepository = ownerRepository;
        this.speciesRepository = speciesRepository;
        this.breedRepository = breedRepository;
        this.treatmentService = treatmentService;
        this.labReportService = labReportService;
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(UUID medicalRecordId, UUID clinicId) {
        try {
            MedicalRecord record = medicalRecordService.findById(medicalRecordId, clinicId);
            Pet pet = record.getPet();
            Owner owner = ownerRepository.findById(pet.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Owner", "id", pet.getOwnerId()));
            String speciesName = pet.getSpeciesId() != null
                    ? speciesRepository.findById(pet.getSpeciesId()).map(s -> s.getName()).orElse("")
                    : "";
            String breedName = pet.getBreedId() != null
                    ? breedRepository.findById(pet.getBreedId()).map(b -> b.getName()).orElse("")
                    : "";

            User vet = record.getVet();
            Clinic clinic = clinicRepository.findById(clinicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Clinic", "id", clinicId));
            List<Prescription> prescriptions = prescriptionService.findByMedicalRecord(clinicId, medicalRecordId);
            List<Vaccination> vaccinations = vaccinationService.findByMedicalRecord(clinicId, medicalRecordId);
            List<Treatment> treatments = treatmentService.findByMedicalRecord(clinicId, medicalRecordId);
            List<LabReport> labReports = labReportService.findByMedicalRecord(clinicId, medicalRecordId);


            Context context = new Context();
            context.setVariable("record", record);
            context.setVariable("pet", pet);
            context.setVariable("vet", vet);
            context.setVariable("clinic", clinic);
            context.setVariable("prescriptions", prescriptions);
            context.setVariable("vaccinations", vaccinations);
            context.setVariable("vetName", vet.getFirstName() + " " + vet.getLastName());
            context.setVariable("owner", owner);
            context.setVariable("speciesName", speciesName);
            context.setVariable("breedName", breedName);
            context.setVariable("treatments", treatments);
            context.setVariable("labReports", labReports);



            String html = templateEngine.process("medical-record", context);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();

            String fontPath = new ClassPathResource("fonts/DejaVuSans.ttf").getURL().toString();
            renderer.getFontResolver().addFont(fontPath, BaseFont.IDENTITY_H, true);

            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Greška pri generisanju PDF-a za medicinski karton: {}", medicalRecordId, e);
            throw new RuntimeException("Greška pri generisanju PDF-a za medicinski karton", e);
        }
    }
}
