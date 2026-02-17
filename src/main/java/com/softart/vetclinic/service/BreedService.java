package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Breed;
import com.softart.vetclinic.exception.DuplicateResourceException;
import com.softart.vetclinic.repository.BreedRepository;
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
public class BreedService extends AbstractCrudService<Breed, BreedRepository> {

    private final BreedRepository breedRepository;
    private final SpeciesRepository speciesRepository;
    private final EntityManager entityManager;

    public BreedService(BreedRepository breedRepository, SpeciesRepository speciesRepository,
                        EntityManager entityManager) {
        super(breedRepository);
        this.breedRepository = breedRepository;
        this.speciesRepository = speciesRepository;
        this.entityManager = entityManager;
    }

    @Override
    protected String getEntityName() {
        return "Breed";
    }

    @Override
    protected Optional<Breed> findByIdAndClinicId(UUID id, UUID clinicId) {
        return breedRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<Breed> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return breedRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return breedRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(Breed entity) {
        requireExists(
                speciesRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getSpeciesId(), entity.getClinicId()),
                "Species", "id", entity.getSpeciesId()
        );
        if (breedRepository.existsBySpeciesIdAndNameAndDeletedFalse(entity.getSpeciesId(), entity.getName())) {
            throw new DuplicateResourceException("Breed", "name", entity.getName());
        }
    }

    @Override
    @Transactional
    public Breed create(Breed entity, UUID clinicId) {
        Breed saved = super.create(entity, clinicId);
        entityManager.flush();
        entityManager.detach(saved);
        return findByIdAndClinicId(saved.getId(), clinicId).orElse(saved);
    }

    @Override
    @Transactional
    public Breed update(UUID id, UUID clinicId, java.util.function.Consumer<Breed> updater) {
        Breed saved = super.update(id, clinicId, updater);
        entityManager.flush();
        entityManager.detach(saved);
        return findByIdAndClinicId(saved.getId(), clinicId).orElse(saved);
    }

    @Transactional(readOnly = true)
    public List<Breed> findBySpecies(UUID clinicId, UUID speciesId) {
        return breedRepository.findByClinicIdAndSpeciesIdAndDeletedFalse(clinicId, speciesId);
    }
}
