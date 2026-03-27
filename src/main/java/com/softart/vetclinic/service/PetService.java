package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Owner;
import com.softart.vetclinic.entity.Pet;
import com.softart.vetclinic.repository.BreedRepository;
import com.softart.vetclinic.repository.OwnerRepository;
import com.softart.vetclinic.repository.PetRepository;
import com.softart.vetclinic.repository.SpeciesRepository;
import jakarta.persistence.EntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PetService extends AbstractCrudService<Pet, PetRepository> {

    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;
    private final SpeciesRepository speciesRepository;
    private final BreedRepository breedRepository;
    private final EntityManager entityManager;

    public PetService(PetRepository petRepository,
                      OwnerRepository ownerRepository,
                      SpeciesRepository speciesRepository,
                      BreedRepository breedRepository,
                      EntityManager entityManager) {
        super(petRepository);
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
        this.speciesRepository = speciesRepository;
        this.breedRepository = breedRepository;
        this.entityManager = entityManager;
    }

    @Override
    protected String getEntityName() {
        return "Pet";
    }

    @Override
    protected Optional<Pet> findByIdAndClinicId(UUID id, UUID clinicId) {
        return petRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<Pet> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return petRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return petRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(Pet entity) {
        requireExists(
                ownerRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getOwnerId(), entity.getClinicId()),
                "Owner", "id", entity.getOwnerId()
        );
        if (entity.getSpeciesId() != null) {
            requireExists(
                    speciesRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getSpeciesId(), entity.getClinicId()),
                    "Species", "id", entity.getSpeciesId()
            );
        }
        if (entity.getBreedId() != null) {
            requireExists(
                    breedRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getBreedId(), entity.getClinicId()),
                    "Breed", "id", entity.getBreedId()
            );
        }
    }

    @Override
    @Transactional
    public Pet create(Pet entity, UUID clinicId) {
        Pet saved = super.create(entity, clinicId);
        entityManager.flush();
        entityManager.detach(saved);
        return findByIdAndClinicId(saved.getId(), clinicId).orElse(saved);
    }

    @Override
    @Transactional
    public Pet update(UUID id, UUID clinicId, java.util.function.Consumer<Pet> updater) {
        Pet saved = super.update(id, clinicId, updater);
        entityManager.flush();
        entityManager.detach(saved);
        return findByIdAndClinicId(saved.getId(), clinicId).orElse(saved);
    }

    @Transactional(readOnly = true)
    public List<Pet> findByOwner(UUID clinicId, UUID ownerId) {
        return petRepository.findByClinicIdAndOwnerIdAndDeletedFalse(clinicId, ownerId);
    }
    
    public Page<Pet> searchAll(UUID clinicId, String search, Pageable pageable) {
    	 if (search == null || search.isBlank()) {
    	        return findAllByClinicId(clinicId, pageable);
    	    }
        return petRepository.searchByClinicId(clinicId, search, pageable);
    }
    
    @Transactional
    public Pet createWithPatientCode(Pet entity, UUID clinicId) {
        if (entity.getPatientCode() == null || entity.getPatientCode().isBlank()) {
            Owner owner = ownerRepository.findByIdAndClinicIdAndDeletedFalse(entity.getOwnerId(), clinicId)
                    .orElse(null);
            if (owner != null && owner.getClientCode() != null && !owner.getClientCode().isBlank()) {
                entity.setPatientCode(generateNextPatientCode(clinicId, owner.getClientCode()));
            }
        }
        return create(entity, clinicId);
    }

    private String generateNextPatientCode(UUID clinicId, String ownerClientCode) {
        String prefix = ownerClientCode + "-";
        String maxCode = petRepository.findMaxPatientCodeByPrefix(clinicId, prefix + "%");
        if (maxCode == null) {
            return prefix + "01";
        }
        try {
            String suffix = maxCode.substring(maxCode.lastIndexOf('-') + 1);
            int next = Integer.parseInt(suffix) + 1;
            return prefix + String.format("%02d", next);
        } catch (NumberFormatException e) {
            return prefix + "01";
        }
    }


}
