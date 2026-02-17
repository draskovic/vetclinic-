package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Clinic;
import com.softart.vetclinic.exception.ResourceNotFoundException;
import com.softart.vetclinic.repository.ClinicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class ClinicService {

    private final ClinicRepository clinicRepository;

    @Transactional(readOnly = true)
    public Clinic findById(UUID id) {
        return clinicRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic", "id", id));
    }

    @Transactional(readOnly = true)
    public Page<Clinic> findAll(Pageable pageable) {
        return clinicRepository.findAll(pageable);
    }

    @Transactional
    public Clinic create(Clinic clinic) {
        clinic.setDeleted(false);
        return clinicRepository.save(clinic);
    }

    @Transactional
    public Clinic update(UUID id, Consumer<Clinic> updater) {
        Clinic existing = findById(id);
        updater.accept(existing);
        return clinicRepository.save(existing);
    }

    @Transactional
    public void softDelete(UUID id) {
        Clinic clinic = findById(id);
        clinic.setDeleted(true);
        clinic.setDeletedAt(OffsetDateTime.now());
        clinicRepository.save(clinic);
    }
    
    @Transactional(readOnly = true)
    public Clinic findByEmail(String email) {
        return clinicRepository.findByEmailBypassRls(email)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic", "email", email));
    }
}
