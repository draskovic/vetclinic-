package com.softart.vetclinic.service;

import com.softart.vetclinic.dto.ImportOwnerRequest;
import com.softart.vetclinic.dto.ImportPetData;
import com.softart.vetclinic.dto.ImportResultResponse;
import com.softart.vetclinic.dto.ImportResultResponse.ImportError;
import com.softart.vetclinic.entity.Breed;
import com.softart.vetclinic.entity.Owner;
import com.softart.vetclinic.entity.Pet;
import com.softart.vetclinic.entity.Species;
import com.softart.vetclinic.repository.BreedRepository;
import com.softart.vetclinic.repository.OwnerRepository;
import com.softart.vetclinic.repository.PetRepository;
import com.softart.vetclinic.repository.SpeciesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OwnerImportService {

    private final OwnerRepository ownerRepository;
    private final PetRepository petRepository;
    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;
    private final PetService petService;

    // Keš za species/breed unutar jednog importa (da ne pravi duplikate)
    private final Map<String, Species> speciesCache = new HashMap<>();
    private final Map<String, Breed> breedCache = new HashMap<>();

    @Transactional
    public ImportResultResponse importOwners(UUID clinicId, List<ImportOwnerRequest> requests) {
        int created = 0;
        int skipped = 0;
        List<ImportError> errors = new ArrayList<>();

        // Očisti keš na početku importa
        speciesCache.clear();
        breedCache.clear();

        for (ImportOwnerRequest req : requests) {
            String ownerName = req.firstName() + " " + req.lastName();
            try {
                // Proveri duplikat po clientCode
                if (req.clientCode() != null && !req.clientCode().isBlank()) {
                    Optional<Owner> existing = ownerRepository
                            .findByClinicIdAndClientCodeAndDeletedFalse(clinicId, req.clientCode());
                    if (existing.isPresent()) {
                        skipped++;
                        continue;
                    }
                }

                // Kreiraj vlasnika
                Owner owner = new Owner();
                owner.setClinicId(clinicId);
                owner.setFirstName(req.firstName().trim());
                owner.setLastName(req.lastName() != null ? req.lastName().trim() : "");
                owner.setPhone(req.phone() != null ? req.phone().trim() : "");
                owner.setAddress(req.address() != null ? req.address().trim() : null);
                owner.setCity(req.city() != null ? req.city().trim() : null);
                owner.setClientCode(req.clientCode() != null ? req.clientCode().trim() : null);
                Owner savedOwner = ownerRepository.save(owner);

                
                // Kreiraj ljubimca sa novim patientCode i legacyCode
                if (req.pets() != null) {
                    for (ImportPetData petData : req.pets()) {
                        if (petData.name() == null || petData.name().isBlank()) continue;

                        Pet pet = new Pet();
                        pet.setClinicId(clinicId);
                        pet.setOwnerId(savedOwner.getId());
                        pet.setName(petData.name().trim());

                        // Resoluj species
                        if (petData.species() != null && !petData.species().isBlank()) {
                            Species species = resolveSpecies(clinicId, petData.species().trim());
                            pet.setSpeciesId(species.getId());

                            // Resoluj breed
                            if (petData.breed() != null && !petData.breed().isBlank()) {
                                Breed breed = resolveBreed(clinicId, species.getId(), petData.breed().trim());
                                pet.setBreedId(breed.getId());
                            }
                        }

                     // Odredi početni broj za patientCode
                        pet.setLegacyCode(petData.legacyCode());
                        petService.createWithPatientCode(pet, clinicId);
                    }
                }

                created++;
            } catch (Exception e) {
                log.error("Import greška za vlasnika {} ({}): {}", ownerName, req.clientCode(), e.getMessage());
                errors.add(new ImportError(req.clientCode(), ownerName, e.getMessage()));
            }
        }

        log.info("Import završen: {} obrađeno, {} kreirano, {} preskočeno, {} grešaka",
                requests.size(), created, skipped, errors.size());

        return new ImportResultResponse(requests.size(), created, skipped, errors);
    }

    private Species resolveSpecies(UUID clinicId, String name) {
        String key = name.toLowerCase();
        if (speciesCache.containsKey(key)) {
            return speciesCache.get(key);
        }

        Species species = speciesRepository
                .findByClinicIdAndNameIgnoreCaseAndDeletedFalse(clinicId, name)
                .orElseGet(() -> {
                    Species s = new Species();
                    s.setClinicId(clinicId);
                    s.setName(name);
                    s.setActive(true);
                    return speciesRepository.save(s);
                });

        speciesCache.put(key, species);
        return species;
    }

    private Breed resolveBreed(UUID clinicId, UUID speciesId, String name) {
        String key = speciesId + ":" + name.toLowerCase();
        if (breedCache.containsKey(key)) {
            return breedCache.get(key);
        }

        Breed breed = breedRepository
                .findBySpeciesIdAndNameIgnoreCaseAndDeletedFalse(speciesId, name)
                .orElseGet(() -> {
                    Breed b = new Breed();
                    b.setClinicId(clinicId);
                    b.setSpeciesId(speciesId);
                    b.setName(name);
                    return breedRepository.save(b);
                });

        breedCache.put(key, breed);
        return breed;
    }
}
