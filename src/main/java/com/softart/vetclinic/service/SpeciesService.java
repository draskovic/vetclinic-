package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Species;
import com.softart.vetclinic.exception.DuplicateResourceException;
import com.softart.vetclinic.repository.SpeciesRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class SpeciesService extends AbstractCrudService<Species, SpeciesRepository> {

    private final SpeciesRepository speciesRepository;

    public SpeciesService(SpeciesRepository speciesRepository) {
        super(speciesRepository);
        this.speciesRepository = speciesRepository;
    }

    @Override
    protected String getEntityName() {
        return "Species";
    }

    @Override
    protected Optional<Species> findByIdAndClinicId(UUID id, UUID clinicId) {
        return speciesRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<Species> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return speciesRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return speciesRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(Species entity) {
        if (speciesRepository.existsByClinicIdAndNameAndDeletedFalse(entity.getClinicId(), entity.getName())) {
            throw new DuplicateResourceException("Species", "name", entity.getName());
        }
    }
}
