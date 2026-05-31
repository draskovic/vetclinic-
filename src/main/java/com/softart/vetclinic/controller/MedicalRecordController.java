package com.softart.vetclinic.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import java.util.ArrayList;
import com.softart.vetclinic.dto.DiagnosisResponse;
import com.softart.vetclinic.entity.Diagnosis;
import com.softart.vetclinic.entity.MedicalRecordDiagnosis;
import com.softart.vetclinic.mapper.DiagnosisMapper;
import com.softart.vetclinic.repository.DiagnosisRepository;
import com.softart.vetclinic.repository.MedicalRecordDiagnosisRepository;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.security.access.prepost.PreAuthorize;

import com.softart.vetclinic.dto.CreateMedicalRecordRequest;
import com.softart.vetclinic.dto.MedicalRecordResponse;
import com.softart.vetclinic.dto.UpdateMedicalRecordRequest;
import com.softart.vetclinic.entity.MedicalRecord;
import com.softart.vetclinic.enums.AppointmentStatus;
import com.softart.vetclinic.exception.ResourceNotFoundException;
import com.softart.vetclinic.mapper.MedicalRecordMapper;
import com.softart.vetclinic.repository.OwnerRepository;
import com.softart.vetclinic.repository.PetRepository;
import com.softart.vetclinic.repository.UserRepository;
import com.softart.vetclinic.service.AppointmentService;
import com.softart.vetclinic.service.MedicalRecordPdfService;
import com.softart.vetclinic.service.MedicalRecordService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final MedicalRecordMapper medicalRecordMapper;
    private final MedicalRecordPdfService medicalRecordPdfService;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final OwnerRepository ownerRepository;
    private final AppointmentService appointmentService;
    private final MedicalRecordDiagnosisRepository medicalRecordDiagnosisRepository;
    private final DiagnosisRepository diagnosisRepository;
    private final DiagnosisMapper diagnosisMapper;
    private final com.softart.vetclinic.service.PetHealthAlertService petHealthAlertService;

    @GetMapping("/{id}")
    public MedicalRecordResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        MedicalRecord r = medicalRecordService.findById(id, clinicId);
        var pet = petRepository.findById(r.getPetId()).orElse(null);
        var owner = pet != null ? ownerRepository.findById(pet.getOwnerId()).orElse(null) : null;
        var vet = userRepository.findById(r.getVetId()).orElse(null);

        return new MedicalRecordResponse(
                r.getId(),
                r.getAppointmentId(),
                r.getRecordCode(),
                r.getPetId(),
                pet != null ? pet.getName() : "",
                pet != null ? pet.getOwnerId() : null,
                owner != null ? owner.getFirstName() + " " + owner.getLastName() : "",
                r.getVetId(),
                vet != null ? vet.getFirstName() + " " + vet.getLastName() : "",
                r.getSymptoms(),
                resolveDiagnoses(clinicId, r.getId()),
                r.getExaminationNotes(),
                r.getWeightKg(),
                r.getTemperatureC(),
                r.getHeartRate(),
                r.getFollowUpRecommended(),
                r.getFollowUpDate(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                false
        );
    }


    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public MedicalRecordResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateMedicalRecordRequest request) {
        var entity = medicalRecordMapper.toEntity(request);
        MedicalRecord r = medicalRecordService.createWithRecordCode(entity, clinicId);
        var pet = petRepository.findById(r.getPetId()).orElse(null);
        var owner = pet != null ? ownerRepository.findById(pet.getOwnerId()).orElse(null) : null;
        var vet = userRepository.findById(r.getVetId()).orElse(null);
        
        // Sačuvaj dijagnoze u junction tabeli
        if (request.diagnosisIds() != null) {
            for (UUID diagId : request.diagnosisIds()) {
                MedicalRecordDiagnosis mrd = new MedicalRecordDiagnosis();
                mrd.setClinicId(clinicId);
                mrd.setMedicalRecordId(r.getId());
                mrd.setDiagnosisId(diagId);
                medicalRecordDiagnosisRepository.save(mrd);
            }
        }

        // Atomic: ako termin update pukne, rollback ceo create (intervencija + dijagnoze).
        // Eliminisan silent try/catch — bolje fail-fast nego polovično stanje.
        if (entity.getAppointmentId() != null) {
            appointmentService.update(entity.getAppointmentId(), clinicId,
                appointment -> appointment.setStatus(AppointmentStatus.COMPLETED));
        }



        return new MedicalRecordResponse(
                r.getId(), r.getAppointmentId(),
                r.getRecordCode(),
                r.getPetId(),
                pet != null ? pet.getName() : "",
                pet != null ? pet.getOwnerId() : null,
                owner != null ? owner.getFirstName() + " " + owner.getLastName() : "",
                r.getVetId(),
                vet != null ? vet.getFirstName() + " " + vet.getLastName() : "",
                r.getSymptoms(),  resolveDiagnoses(clinicId, r.getId()), r.getExaminationNotes(),
                r.getWeightKg(), r.getTemperatureC(), r.getHeartRate(),
                r.getFollowUpRecommended(), r.getFollowUpDate(),
                r.getCreatedAt(), r.getUpdatedAt(), false
        );
    }

    @PostMapping("/start-from-appointment/{appointmentId}")
    @Transactional
    public MedicalRecordResponse startFromAppointment(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID appointmentId) {

        // Ako već postoji intervencija za ovaj termin, vrati je
        Optional<MedicalRecord> existing = medicalRecordService.findByAppointment(appointmentId);
        if (existing.isPresent()) {
            MedicalRecord r = existing.get();
            var pet = petRepository.findById(r.getPetId()).orElse(null);
            var owner = pet != null ? ownerRepository.findById(pet.getOwnerId()).orElse(null) : null;
            var vet = userRepository.findById(r.getVetId()).orElse(null);
            return new MedicalRecordResponse(
                    r.getId(), r.getAppointmentId(), r.getRecordCode(),
                    r.getPetId(), pet != null ? pet.getName() : "",
                    pet != null ? pet.getOwnerId() : null,
                    owner != null ? owner.getFirstName() + " " + owner.getLastName() : "",
                    r.getVetId(), vet != null ? vet.getFirstName() + " " + vet.getLastName() : "",
                    r.getSymptoms(), resolveDiagnoses(clinicId, r.getId()), r.getExaminationNotes(),
                    r.getWeightKg(), r.getTemperatureC(), r.getHeartRate(),
                    r.getFollowUpRecommended(), r.getFollowUpDate(),
                    r.getCreatedAt(), r.getUpdatedAt(),false
            );
        }

        // Dohvati termin
        var appointment = appointmentService.findById(appointmentId, clinicId);

        // Kreiraj draft MedicalRecord
        MedicalRecord entity = new MedicalRecord();
        entity.setAppointmentId(appointmentId);
        entity.setPetId(appointment.getPetId());
        entity.setVetId(appointment.getVetId());
        entity.setSymptoms(appointment.getReason());
        entity.setFollowUpRecommended(false);

        MedicalRecord r = medicalRecordService.createWithRecordCode(entity, clinicId);

        // Atomic: ako termin update pukne, rollback ceo startFromAppointment.
        // Eliminisan silent try/catch — bolje fail-fast nego polovično stanje (draft intervencija + termin u starom statusu).
        appointmentService.update(appointmentId, clinicId,
            a -> a.setStatus(AppointmentStatus.IN_PROGRESS));

        var pet = petRepository.findById(r.getPetId()).orElse(null);
        var owner = pet != null ? ownerRepository.findById(pet.getOwnerId()).orElse(null) : null;
        var vet = userRepository.findById(r.getVetId()).orElse(null);

        return new MedicalRecordResponse(
                r.getId(), r.getAppointmentId(), r.getRecordCode(),
                r.getPetId(), pet != null ? pet.getName() : "",
                pet != null ? pet.getOwnerId() : null,
                owner != null ? owner.getFirstName() + " " + owner.getLastName() : "",
                r.getVetId(), vet != null ? vet.getFirstName() + " " + vet.getLastName() : "",
                r.getSymptoms(), resolveDiagnoses(clinicId, r.getId()), r.getExaminationNotes(),
                r.getWeightKg(), r.getTemperatureC(), r.getHeartRate(),
                r.getFollowUpRecommended(), r.getFollowUpDate(),
                r.getCreatedAt(), r.getUpdatedAt(), false
        );
    }


    @PutMapping("/{id}")
    @Transactional
    public MedicalRecordResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMedicalRecordRequest request) {
        MedicalRecord r = medicalRecordService.update(id, clinicId, existing -> medicalRecordMapper.updateEntity(request, existing));
        // Replace-all dijagnoze
        if (request.diagnosisIds() != null) {
            medicalRecordDiagnosisRepository.deleteByClinicIdAndMedicalRecordId(clinicId, id);
            for (UUID diagId : request.diagnosisIds()) {
                MedicalRecordDiagnosis mrd = new MedicalRecordDiagnosis();
                mrd.setClinicId(clinicId);
                mrd.setMedicalRecordId(id);
                mrd.setDiagnosisId(diagId);
                medicalRecordDiagnosisRepository.save(mrd);
            }
        }

        var pet = petRepository.findById(r.getPetId()).orElse(null);
        var owner = pet != null ? ownerRepository.findById(pet.getOwnerId()).orElse(null) : null;
        var vet = userRepository.findById(r.getVetId()).orElse(null);

        return new MedicalRecordResponse(
                r.getId(), r.getAppointmentId(),  r.getRecordCode(),
                r.getPetId(),
                pet != null ? pet.getName() : "",
                pet != null ? pet.getOwnerId() : null,
                owner != null ? owner.getFirstName() + " " + owner.getLastName() : "",
                r.getVetId(),
                vet != null ? vet.getFirstName() + " " + vet.getLastName() : "",
                r.getSymptoms(), resolveDiagnoses(clinicId, r.getId()), r.getExaminationNotes(),
                r.getWeightKg(), r.getTemperatureC(), r.getHeartRate(),
                r.getFollowUpRecommended(), r.getFollowUpDate(),
                r.getCreatedAt(), r.getUpdatedAt(), false
        );
    }

    @PostMapping("/{id}/finish")
    @Transactional
    @PreAuthorize("hasAuthority('manage_medical_records') or hasAuthority('*')")
    public MedicalRecordResponse finish(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMedicalRecordRequest request) {

        // 1. Update intervencije sa form values (kao u standard update endpoint-u)
        MedicalRecord r = medicalRecordService.update(id, clinicId,
                existing -> medicalRecordMapper.updateEntity(request, existing));

        // 2. Replace-all dijagnoza (identičan flow kao u update endpoint-u)
        if (request.diagnosisIds() != null) {
            medicalRecordDiagnosisRepository.deleteByClinicIdAndMedicalRecordId(clinicId, id);
            for (UUID diagId : request.diagnosisIds()) {
                MedicalRecordDiagnosis mrd = new MedicalRecordDiagnosis();
                mrd.setClinicId(clinicId);
                mrd.setMedicalRecordId(id);
                mrd.setDiagnosisId(diagId);
                medicalRecordDiagnosisRepository.save(mrd);
            }
        }

        // 3. ATOMIC: ako intervencija ima vezan termin, postavi status na COMPLETED.
        // BEZ try/catch — ako appointment update pukne (validation, RLS, network),
        // CEO finish flow se rollback-uje (intervencija ostaje neizmenjena, dijagnoze takođe).
        // Eliminiše polovično stanje koje je postojalo u handleFinish-u sa silent try/catch.
        if (r.getAppointmentId() != null) {
            appointmentService.update(r.getAppointmentId(), clinicId,
                    a -> a.setStatus(AppointmentStatus.COMPLETED));
        }

        // 4. Build response (identičan format kao update endpoint)
        var pet = petRepository.findById(r.getPetId()).orElse(null);
        var owner = pet != null ? ownerRepository.findById(pet.getOwnerId()).orElse(null) : null;
        var vet = userRepository.findById(r.getVetId()).orElse(null);

        return new MedicalRecordResponse(
                r.getId(), r.getAppointmentId(), r.getRecordCode(),
                r.getPetId(),
                pet != null ? pet.getName() : "",
                pet != null ? pet.getOwnerId() : null,
                owner != null ? owner.getFirstName() + " " + owner.getLastName() : "",
                r.getVetId(),
                vet != null ? vet.getFirstName() + " " + vet.getLastName() : "",
                r.getSymptoms(), resolveDiagnoses(clinicId, r.getId()), r.getExaminationNotes(),
                r.getWeightKg(), r.getTemperatureC(), r.getHeartRate(),
                r.getFollowUpRecommended(), r.getFollowUpDate(),
                r.getCreatedAt(), r.getUpdatedAt(),false
        );
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        medicalRecordService.softDelete(id, clinicId);
    }

    @GetMapping("/by-pet/{petId}")
    public List<MedicalRecordResponse> getByPet(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID petId) {
        List<MedicalRecord> records = medicalRecordService.findByPet(clinicId, petId);
        var pet = petRepository.findById(petId).orElse(null);
        var owner = pet != null ? ownerRepository.findById(pet.getOwnerId()).orElse(null) : null;

        Set<UUID> vetIds = records.stream().map(MedicalRecord::getVetId).collect(Collectors.toSet());
        Map<UUID, String> vetNames = userRepository.findAllById(vetIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u.getFirstName() + " " + u.getLastName()));
        
        List<UUID> recordIds = records.stream().map(MedicalRecord::getId).toList();
        Map<UUID, List<DiagnosisResponse>> diagnosesMap = resolveDiagnosesBatch(clinicId, recordIds);
        boolean petHasAlerts = !petHealthAlertService.findActiveByPet(clinicId, petId).isEmpty();

        return records.stream().map(r -> new MedicalRecordResponse(
                r.getId(), r.getAppointmentId(),  r.getRecordCode(),
                r.getPetId(),
                pet != null ? pet.getName() : "",
                pet != null ? pet.getOwnerId() : null,
                owner != null ? owner.getFirstName() + " " + owner.getLastName() : "",
                r.getVetId(),
                vetNames.getOrDefault(r.getVetId(), ""),
                r.getSymptoms(), 
                diagnosesMap.getOrDefault(r.getId(), List.of()),
                r.getExaminationNotes(),
                r.getWeightKg(), r.getTemperatureC(), r.getHeartRate(),
                r.getFollowUpRecommended(), r.getFollowUpDate(),
                r.getCreatedAt(), r.getUpdatedAt(),
                petHasAlerts
        )).toList();
    }


    @GetMapping("/by-appointment/{appointmentId}")
    public MedicalRecordResponse getByAppointment(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID appointmentId) {
        MedicalRecord r = medicalRecordService.findByAppointment(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", "appointmentId", appointmentId));
        var pet = petRepository.findById(r.getPetId()).orElse(null);
        var owner = pet != null ? ownerRepository.findById(pet.getOwnerId()).orElse(null) : null;
        var vet = userRepository.findById(r.getVetId()).orElse(null);

        return new MedicalRecordResponse(
                r.getId(), r.getAppointmentId(),  r.getRecordCode(), r.getPetId(),
                pet != null ? pet.getName() : "",
                pet != null ? pet.getOwnerId() : null,
                owner != null ? owner.getFirstName() + " " + owner.getLastName() : "",
                r.getVetId(),
                vet != null ? vet.getFirstName() + " " + vet.getLastName() : "",
                r.getSymptoms(), resolveDiagnoses(clinicId, r.getId()), r.getExaminationNotes(),
                r.getWeightKg(), r.getTemperatureC(), r.getHeartRate(),
                r.getFollowUpRecommended(), r.getFollowUpDate(),
                r.getCreatedAt(), r.getUpdatedAt(), false
        );
    }

    
    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> generatePdf(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        byte[] pdf = medicalRecordPdfService.generatePdf(id, clinicId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "karton-" + id + ".pdf");
        return ResponseEntity.ok().headers(headers).body(pdf);
    }
    
    @GetMapping("/by-owner/{ownerId}")
    public List<MedicalRecordResponse> getByOwner(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID ownerId) {
        List<MedicalRecord> records = medicalRecordService.findByOwner(clinicId, ownerId);

        String ownerFullName = ownerRepository.findById(ownerId)
                .map(o -> o.getFirstName() + " " + o.getLastName())
                .orElse("");
        
        Set<UUID> petIds = records.stream().map(MedicalRecord::getPetId).collect(Collectors.toSet());
        Map<UUID, String> petNames = petRepository.findAllById(petIds).stream()
                .collect(Collectors.toMap(p -> p.getId(), p -> p.getName()));

        Set<UUID> vetIds = records.stream().map(MedicalRecord::getVetId).collect(Collectors.toSet());
        Map<UUID, String> vetNames = userRepository.findAllById(vetIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u.getFirstName() + " " + u.getLastName()));
        
        List<UUID> recordIds = records.stream().map(MedicalRecord::getId).toList();
        Map<UUID, List<DiagnosisResponse>> diagnosesMap = resolveDiagnosesBatch(clinicId, recordIds);
        Set<UUID> ownerPetIds = records.stream().map(MedicalRecord::getPetId).collect(Collectors.toSet());
        Set<UUID> petsWithAlerts = petHealthAlertService.findPetIdsWithActiveAlerts(clinicId, ownerPetIds);

        return records.stream().map(r -> new MedicalRecordResponse(
                r.getId(),
                r.getAppointmentId(),
                r.getRecordCode(),
                r.getPetId(),
                petNames.getOrDefault(r.getPetId(), ""),
                ownerId,           // <-- novo
                ownerFullName,     // <-- novo
                r.getVetId(),
                vetNames.getOrDefault(r.getVetId(), ""),
                r.getSymptoms(),
                diagnosesMap.getOrDefault(r.getId(), List.of()),
                r.getExaminationNotes(),
                r.getWeightKg(),
                r.getTemperatureC(),
                r.getHeartRate(),
                r.getFollowUpRecommended(),
                r.getFollowUpDate(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                petsWithAlerts.contains(r.getPetId())
        )).toList();
    }
    
    @GetMapping
    public Page<MedicalRecordResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.OffsetDateTime dateFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME) java.time.OffsetDateTime dateTo,
            @RequestParam(required = false) UUID vetId,
            @RequestParam(required = false) UUID ownerId,
            Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        Page<MedicalRecord> page = medicalRecordService.searchAll(clinicId, search, dateFrom, dateTo, vetId,ownerId, pageable);

        Set<UUID> petIds = page.getContent().stream().map(MedicalRecord::getPetId).collect(Collectors.toSet());
        Map<UUID, String> petNames = petRepository.findAllById(petIds).stream()
                .collect(Collectors.toMap(p -> p.getId(), p -> p.getName()));
        Map<UUID, UUID> petOwnerMap = petRepository.findAllById(petIds).stream()
                .collect(Collectors.toMap(p -> p.getId(), p -> p.getOwnerId()));

        Set<UUID> ownerIds = new java.util.HashSet<>(petOwnerMap.values());
        Map<UUID, String> ownerNames = ownerRepository.findAllById(ownerIds).stream()
                .collect(Collectors.toMap(o -> o.getId(), o -> o.getFirstName() + " " + o.getLastName()));

        Set<UUID> vetIds = page.getContent().stream().map(MedicalRecord::getVetId).collect(Collectors.toSet());
        Map<UUID, String> vetNames = userRepository.findAllById(vetIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u.getFirstName() + " " + u.getLastName()));

        List<UUID> recordIds = page.getContent().stream().map(MedicalRecord::getId).toList();
        Map<UUID, List<DiagnosisResponse>> diagnosesMap = resolveDiagnosesBatch(clinicId, recordIds);
        Set<UUID> petsWithAlerts = petHealthAlertService.findPetIdsWithActiveAlerts(clinicId, petIds);
        
        return page.map(r -> new MedicalRecordResponse(
                r.getId(),
                r.getAppointmentId(),
                r.getRecordCode(),
                r.getPetId(),
                petNames.getOrDefault(r.getPetId(), ""),
                petOwnerMap.get(r.getPetId()),
                ownerNames.getOrDefault(petOwnerMap.get(r.getPetId()), ""),
                r.getVetId(),
                vetNames.getOrDefault(r.getVetId(), ""),
                r.getSymptoms(),
                diagnosesMap.getOrDefault(r.getId(), List.of()),
                r.getExaminationNotes(),
                r.getWeightKg(),
                r.getTemperatureC(),
                r.getHeartRate(),
                r.getFollowUpRecommended(),
                r.getFollowUpDate(),
                r.getCreatedAt(),
                r.getUpdatedAt(),
                petsWithAlerts.contains(r.getPetId())
        ));
    }

    private List<DiagnosisResponse> resolveDiagnoses(UUID clinicId, UUID medicalRecordId) {
        List<MedicalRecordDiagnosis> mrdList = medicalRecordDiagnosisRepository
                .findByClinicIdAndMedicalRecordId(clinicId, medicalRecordId);
        if (mrdList.isEmpty()) return List.of();
        List<UUID> diagIds = mrdList.stream().map(MedicalRecordDiagnosis::getDiagnosisId).toList();
        List<Diagnosis> diagnoses = diagnosisRepository.findAllById(diagIds);
        return diagnoses.stream().map(diagnosisMapper::toResponse).toList();
    }

    private Map<UUID, List<DiagnosisResponse>> resolveDiagnosesBatch(UUID clinicId, List<UUID> medicalRecordIds) {
        if (medicalRecordIds.isEmpty()) return Map.of();
        List<MedicalRecordDiagnosis> allMrd = medicalRecordDiagnosisRepository
                .findByClinicIdAndMedicalRecordIdIn(clinicId, medicalRecordIds);
        if (allMrd.isEmpty()) return Map.of();
        Set<UUID> diagIds = allMrd.stream().map(MedicalRecordDiagnosis::getDiagnosisId).collect(Collectors.toSet());
        Map<UUID, Diagnosis> diagMap = diagnosisRepository.findAllById(new ArrayList<>(diagIds))
                .stream().collect(Collectors.toMap(Diagnosis::getId, d -> d));
        return allMrd.stream().collect(Collectors.groupingBy(
                MedicalRecordDiagnosis::getMedicalRecordId,
                Collectors.mapping(mrd -> diagnosisMapper.toResponse(diagMap.get(mrd.getDiagnosisId())),
                        Collectors.toList())
        ));
    }

}
