package com.softart.vetclinic.service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.softart.vetclinic.entity.MedicalRecord;
import com.softart.vetclinic.repository.AppointmentRepository;
import com.softart.vetclinic.repository.MedicalRecordRepository;
import com.softart.vetclinic.repository.PetRepository;
import com.softart.vetclinic.repository.UserRepository;
import com.softart.vetclinic.repository.ClinicLocationRepository;
import org.springframework.data.jpa.domain.Specification;
import static com.softart.vetclinic.repository.specifications.MedicalRecordSpecifications.*;


@Service
public class MedicalRecordService extends AbstractCrudService<MedicalRecord, MedicalRecordRepository> {

    private final MedicalRecordRepository medicalRecordRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final ClinicLocationRepository clinicLocationRepository;

    public MedicalRecordService(MedicalRecordRepository medicalRecordRepository,
                                PetRepository petRepository,
                                UserRepository userRepository,
                                AppointmentRepository appointmentRepository,
                                ClinicLocationRepository clinicLocationRepository) {
        super(medicalRecordRepository);
        this.medicalRecordRepository = medicalRecordRepository;
        this.petRepository = petRepository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.clinicLocationRepository = clinicLocationRepository;
    }

    @Override
    protected String getEntityName() {
        return "MedicalRecord";
    }

    @Override
    protected Optional<MedicalRecord> findByIdAndClinicId(UUID id, UUID clinicId) {
        return medicalRecordRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<MedicalRecord> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return medicalRecordRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return medicalRecordRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(MedicalRecord entity) {
        requireExists(
                petRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getPetId(), entity.getClinicId()),
                "Pet", "id", entity.getPetId()
        );
        requireExists(
                userRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getVetId(), entity.getClinicId()),
                "User", "id", entity.getVetId()
        );
        if (entity.getAppointmentId() != null) {
            requireExists(
                    appointmentRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getAppointmentId(), entity.getClinicId()),
                    "Appointment", "id", entity.getAppointmentId()
            );
        }
    }

    @Transactional(readOnly = true)
    public List<MedicalRecord> findByPet(UUID clinicId, UUID petId) {
        return medicalRecordRepository.findByClinicIdAndPetIdAndDeletedFalseOrderByCreatedAtDesc(clinicId, petId);
    }

    @Transactional(readOnly = true)
    public Optional<MedicalRecord> findByAppointment(UUID appointmentId) {
        return medicalRecordRepository.findByAppointmentIdAndDeletedFalse(appointmentId);
    }
    
    @Transactional(readOnly = true)
    public List<MedicalRecord> findByOwner(UUID clinicId, UUID ownerId) {
        return medicalRecordRepository.findByClinicIdAndOwnerIdOrderByCreatedAtDesc(clinicId, ownerId);
    }

    @Transactional(readOnly = true)
    public Page<MedicalRecord> searchAll(UUID clinicId, String search,
            OffsetDateTime dateFrom, OffsetDateTime dateTo, UUID vetId, UUID ownerId, Pageable pageable) {

//        Specification<MedicalRecord> spec1 = Specification.where(inClinic(clinicId))
//                .and(notDeleted())
//                .and(withEagerLoading())
//                .and(textSearch(search))
//                .and(dateBetween(dateFrom, dateTo))
//                .and(vetId(vetId))
//                .and(ownerId(ownerId));
     // umesto Specification.where(inClinic(clinicId)).and(...)
        Specification<MedicalRecord> spec = Specification.allOf(
                inClinic(clinicId), notDeleted(), withEagerLoading(),
                textSearch(search), dateBetween(dateFrom, dateTo), vetId(vetId), ownerId(ownerId));

        return medicalRecordRepository.findAll(spec, pageable);
    }
    
    @Transactional
    public MedicalRecord createWithRecordCode(MedicalRecord entity, UUID clinicId) {
        entity.setRecordCode(generateNextRecordCode(clinicId));
        if (entity.getLocationId() == null) {
            entity.setLocationId(resolveLocationId(entity, clinicId));
        }
        return create(entity, clinicId);
    }

    /**
     * Lokacija intervencije: termin (appointment.location_id) → glavna lokacija klinike.
     * Vraća null samo ako klinika nema glavnu lokaciju (dedukcija ima dalji fallback — Korak 5).
     */
    private UUID resolveLocationId(MedicalRecord entity, UUID clinicId) {
        if (entity.getAppointmentId() != null) {
            UUID fromAppointment = appointmentRepository
                    .findByIdAndClinicIdAndDeletedFalse(entity.getAppointmentId(), clinicId)
                    .map(a -> a.getLocationId())
                    .orElse(null);
            if (fromAppointment != null) return fromAppointment;
        }
        return clinicLocationRepository
                .findFirstByClinicIdAndIsMainTrueAndDeletedFalseOrderByCreatedAtAsc(clinicId)
                .map(loc -> loc.getId())
                .orElse(null);
    }

    private String generateNextRecordCode(UUID clinicId) {
        String yearSuffix = String.valueOf(java.time.LocalDate.now().getYear() % 100);
        String prefix = "I" + yearSuffix + "-";
        String maxCode = medicalRecordRepository.findMaxRecordCodeByPrefix(clinicId, prefix);
        int nextNumber = 1;
        if (maxCode != null) {
            String numberPart = maxCode.substring(prefix.length());
            nextNumber = Integer.parseInt(numberPart) + 1;
        }
        return prefix + String.format("%04d", nextNumber);
    }


}
