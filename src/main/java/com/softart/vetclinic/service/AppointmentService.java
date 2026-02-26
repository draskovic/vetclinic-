package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Appointment;
import com.softart.vetclinic.exception.BadRequestException;
import com.softart.vetclinic.repository.AppointmentRepository;
import com.softart.vetclinic.repository.ClinicLocationRepository;
import com.softart.vetclinic.repository.OwnerRepository;
import com.softart.vetclinic.repository.PetRepository;
import com.softart.vetclinic.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class AppointmentService extends AbstractCrudService<Appointment, AppointmentRepository> {

    private final AppointmentRepository appointmentRepository;
    private final PetRepository petRepository;
    private final OwnerRepository ownerRepository;
    private final UserRepository userRepository;
    private final ClinicLocationRepository clinicLocationRepository;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              PetRepository petRepository,
                              OwnerRepository ownerRepository,
                              UserRepository userRepository,
                              ClinicLocationRepository clinicLocationRepository) {
        super(appointmentRepository);
        this.appointmentRepository = appointmentRepository;
        this.petRepository = petRepository;
        this.ownerRepository = ownerRepository;
        this.userRepository = userRepository;
        this.clinicLocationRepository = clinicLocationRepository;
    }

    @Override
    protected String getEntityName() {
        return "Appointment";
    }

    @Override
    protected Optional<Appointment> findByIdAndClinicId(UUID id, UUID clinicId) {
        return appointmentRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<Appointment> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return appointmentRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return appointmentRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(Appointment entity) {
        validateAppointment(entity, null);
    }

    @Override
    protected void validateForUpdate(Appointment existing, Appointment updated) {
        validateAppointment(updated, existing.getId());
    }

    private void validateAppointment(Appointment entity, UUID excludeId) {
        requireExists(
                petRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getPetId(), entity.getClinicId()),
                "Pet", "id", entity.getPetId()
        );
        requireExists(
                ownerRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getOwnerId(), entity.getClinicId()),
                "Owner", "id", entity.getOwnerId()
        );
        requireExists(
                userRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getVetId(), entity.getClinicId()),
                "User", "id", entity.getVetId()
        );
        requireExists(
                clinicLocationRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getLocationId(), entity.getClinicId()),
                "ClinicLocation", "id", entity.getLocationId()
        );

        if (!entity.getEndTime().isAfter(entity.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        if (appointmentRepository.hasOverlappingAppointment(
                entity.getClinicId(), entity.getVetId(),
                entity.getStartTime(), entity.getEndTime(), excludeId)) {
            throw new BadRequestException("Vet has an overlapping appointment in the requested time slot");
        }
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByDateRange(UUID clinicId, OffsetDateTime from, OffsetDateTime to) {
        return appointmentRepository.findByClinicIdAndDeletedFalseAndStartTimeBetween(clinicId, from, to);
    }

    @Transactional(readOnly = true)
    public List<Appointment> findByVetAndDateRange(UUID clinicId, UUID vetId, OffsetDateTime from, OffsetDateTime to) {
        return appointmentRepository.findByClinicIdAndDeletedFalseAndVetIdAndStartTimeBetween(clinicId, vetId, from, to);
    }
    
    @Transactional(readOnly = true)
    public List<Appointment> findByPet(UUID clinicId, UUID petId) {                                                           
        return appointmentRepository.findByClinicIdAndPetIdAndDeletedFalseOrderByStartTimeDesc(clinicId, petId);
    }
}
