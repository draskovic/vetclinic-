package com.softart.vetclinic.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softart.vetclinic.dto.ImportResultResponse;
import com.softart.vetclinic.dto.ImportResultResponse.ImportError;
import com.softart.vetclinic.entity.Breed;
import com.softart.vetclinic.exception.ResourceNotFoundException;
import com.softart.vetclinic.repository.BreedRepository;
import com.softart.vetclinic.repository.SpeciesRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class BreedSeedService {

    private static final String DOG_BREEDS_RESOURCE = "seed/dog-breeds-sr.json";

    private final BreedRepository breedRepository;
    private final SpeciesRepository speciesRepository;
    private final ObjectMapper objectMapper;

    public ImportResultResponse seedDogBreeds(UUID clinicId, UUID speciesId) {
        if (!speciesRepository.existsByIdAndClinicIdAndDeletedFalse(speciesId, clinicId)) {
            throw new ResourceNotFoundException("Species", "id", speciesId);
        }

        SeedFile data = loadSeedFile(DOG_BREEDS_RESOURCE);
        List<String> breeds = data.breeds();

        int created = 0;
        int skipped = 0;
        List<ImportError> errors = new ArrayList<>();

        for (String raw : breeds) {
            String name = raw == null ? "" : raw.trim();
            if (name.isEmpty()) {
                continue;
            }
            try {
                if (breedRepository.existsBySpeciesIdAndNameAndDeletedFalse(speciesId, name)) {
                    skipped++;
                    continue;
                }
                Breed breed = new Breed();
                breed.setClinicId(clinicId);
                breed.setSpeciesId(speciesId);
                breed.setName(name);
                breedRepository.save(breed);
                created++;
            } catch (Exception e) {
                log.error("Greška kod rase '{}': {}", name, e.getMessage());
                errors.add(new ImportError(null, name, e.getMessage()));
            }
        }

        log.info("Seed pasa završen: ukupno={}, kreirano={}, preskočeno={}, grešaka={}",
                breeds.size(), created, skipped, errors.size());

        return new ImportResultResponse(breeds.size(), created, skipped, errors);
    }

    private SeedFile loadSeedFile(String path) {
        try (InputStream in = new ClassPathResource(path).getInputStream()) {
            return objectMapper.readValue(in, SeedFile.class);
        } catch (IOException e) {
            throw new IllegalStateException("Ne mogu da učitam seed fajl: " + path, e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record SeedFile(String species, List<String> breeds) {}
}