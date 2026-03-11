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
import java.util.List;
import java.util.UUID;

@Service
public class VaccinationPdfService {

    private static final Logger log = LoggerFactory.getLogger(VaccinationPdfService.class);

    private final VaccinationService vaccinationService;
    private final PetService petService;
    private final ClinicRepository clinicRepository;
    private final OwnerRepository ownerRepository;
    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;
    private final TemplateEngine templateEngine;

    public VaccinationPdfService(VaccinationService vaccinationService,
                                  PetService petService,
                                  ClinicRepository clinicRepository,
                                  OwnerRepository ownerRepository,
                                  SpeciesRepository speciesRepository,
                                  BreedRepository breedRepository,
                                  TemplateEngine templateEngine) {
        this.vaccinationService = vaccinationService;
        this.petService = petService;
        this.clinicRepository = clinicRepository;
        this.ownerRepository = ownerRepository;
        this.speciesRepository = speciesRepository;
        this.breedRepository = breedRepository;
        this.templateEngine = templateEngine;
    }

    @Transactional(readOnly = true)
    public byte[] generatePdf(UUID petId, UUID clinicId) {
        try {
            Pet pet = petService.findById(petId, clinicId);
            Clinic clinic = clinicRepository.findById(clinicId)
                    .orElseThrow(() -> new ResourceNotFoundException("Clinic", "id", clinicId));
            Owner owner = ownerRepository.findById(pet.getOwnerId())
                    .orElseThrow(() -> new ResourceNotFoundException("Owner", "id", pet.getOwnerId()));
            String speciesName = pet.getSpeciesId() != null
                    ? speciesRepository.findById(pet.getSpeciesId()).map(Species::getName).orElse("")
                    : "";
            String breedName = pet.getBreedId() != null
                    ? breedRepository.findById(pet.getBreedId()).map(Breed::getName).orElse("")
                    : "";
            List<Vaccination> vaccinations = vaccinationService.findByPet(clinicId, petId);

            Context context = new Context();
            context.setVariable("pet", pet);
            context.setVariable("owner", owner);
            context.setVariable("clinic", clinic);
            context.setVariable("vaccinations", vaccinations);
            context.setVariable("speciesName", speciesName);
            context.setVariable("breedName", breedName);

            String html = templateEngine.process("vaccination-list", context);

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ITextRenderer renderer = new ITextRenderer();

            String fontPath = new ClassPathResource("fonts/DejaVuSans.ttf").getURL().toString();
            renderer.getFontResolver().addFont(fontPath, BaseFont.IDENTITY_H, true);

            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(outputStream);

            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Greška pri generisanju PDF-a za vakcinacioni list: petId={}", petId, e);
            throw new RuntimeException("Greška pri generisanju PDF-a za vakcinacioni list", e);
        }
    }
}
