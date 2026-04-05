package com.softart.vetclinic.service;

import com.lowagie.text.pdf.BaseFont;
import com.softart.vetclinic.entity.*;
import com.softart.vetclinic.exception.ResourceNotFoundException;
import com.softart.vetclinic.repository.ClinicRepository;
import com.softart.vetclinic.repository.OwnerRepository;
import com.softart.vetclinic.repository.SpeciesRepository;
import com.softart.vetclinic.repository.BreedRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

@Service
public class PrescriptionPdfService {

    private static final Logger log = LoggerFactory.getLogger(PrescriptionPdfService.class);

    private final PrescriptionService prescriptionService;
    private final PetService petService;
    private final ClinicRepository clinicRepository;
    private final OwnerRepository ownerRepository;
    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;
    private final TemplateEngine templateEngine;
    private final ClinicLogoHelper clinicLogoHelper;


    public PrescriptionPdfService(PrescriptionService prescriptionService,
                                   PetService petService,
                                   ClinicRepository clinicRepository,
                                   OwnerRepository ownerRepository,
                                   SpeciesRepository speciesRepository,
                                   BreedRepository breedRepository,
                                   TemplateEngine templateEngine,
                                   ClinicLogoHelper clinicLogoHelper) {
        this.prescriptionService = prescriptionService;
        this.petService = petService;
        this.clinicRepository = clinicRepository;
        this.ownerRepository = ownerRepository;
        this.speciesRepository = speciesRepository;
        this.breedRepository = breedRepository;
        this.templateEngine = templateEngine;
        this.clinicLogoHelper = clinicLogoHelper;
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(UUID prescriptionId, UUID clinicId) {
        try {
            Prescription prescription = prescriptionService.findById(prescriptionId, clinicId);
            Pet pet = petService.findById(prescription.getPetId(), clinicId);
            Clinic clinic = clinicRepository.findById(clinicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Clinic", "id", clinicId));
            Owner owner = ownerRepository.findById(pet.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Owner", "id", pet.getOwnerId()));
            User vet = prescription.getVet();
            String vetName = vet.getFirstName() + " " + vet.getLastName();
            String speciesName = pet.getSpeciesId() != null
                    ? speciesRepository.findById(pet.getSpeciesId()).map(Species::getName).orElse("")
                    : "";
            String breedName = pet.getBreedId() != null
                    ? breedRepository.findById(pet.getBreedId()).map(Breed::getName).orElse("")
                    : "";

            Context context = new Context();
            context.setVariable("prescription", prescription);
            context.setVariable("pet", pet);
            context.setVariable("owner", owner);
            context.setVariable("clinic", clinic);
            context.setVariable("vetName", vetName);
            context.setVariable("speciesName", speciesName);
            context.setVariable("breedName", breedName);
            context.setVariable("logoDataUri", clinicLogoHelper.toDataUri(clinic));


            String html = templateEngine.process("prescription", context);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();

            String fontPath = new ClassPathResource("fonts/DejaVuSans.ttf").getURL().toString();
            renderer.getFontResolver().addFont(fontPath, BaseFont.IDENTITY_H, true);

            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Greška pri generisanju PDF-a za recept: {}", prescriptionId, e);
            throw new RuntimeException("Greška pri generisanju PDF-a za recept", e);
        }
    }
}
