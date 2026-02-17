package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.UserLocation;
import com.softart.vetclinic.repository.ClinicLocationRepository;
import com.softart.vetclinic.repository.UserLocationRepository;
import com.softart.vetclinic.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserLocationService extends AbstractCrudService<UserLocation, UserLocationRepository> {

    private final UserLocationRepository userLocationRepository;
    private final UserRepository userRepository;
    private final ClinicLocationRepository clinicLocationRepository;

    public UserLocationService(UserLocationRepository userLocationRepository,
                               UserRepository userRepository,
                               ClinicLocationRepository clinicLocationRepository) {
        super(userLocationRepository);
        this.userLocationRepository = userLocationRepository;
        this.userRepository = userRepository;
        this.clinicLocationRepository = clinicLocationRepository;
    }

    @Override
    protected String getEntityName() {
        return "UserLocation";
    }

    @Override
    protected Optional<UserLocation> findByIdAndClinicId(UUID id, UUID clinicId) {
        return userLocationRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<UserLocation> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return userLocationRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return userLocationRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(UserLocation entity) {
        requireExists(
                userRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getUserId(), entity.getClinicId()),
                "User", "id", entity.getUserId()
        );
        requireExists(
                clinicLocationRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getLocationId(), entity.getClinicId()),
                "ClinicLocation", "id", entity.getLocationId()
        );
    }

    @Transactional(readOnly = true)
    public List<UserLocation> findByUser(UUID clinicId, UUID userId) {
        return userLocationRepository.findByClinicIdAndUserIdAndDeletedFalse(clinicId, userId);
    }
}
