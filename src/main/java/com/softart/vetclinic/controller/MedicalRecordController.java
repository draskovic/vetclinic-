package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateMedicalRecordRequest;
import com.softart.vetclinic.repository.PetRepository;
import com.softart.vetclinic.repository.UserRepository;
import com.softart.vetclinic.entity.MedicalRecord;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import com.softart.vetclinic.dto.MedicalRecordResponse;
import com.softart.vetclinic.dto.UpdateMedicalRecordRequest;
import com.softart.vetclinic.exception.ResourceNotFoundException;
import com.softart.vetclinic.mapper.MedicalRecordMapper;
import com.softart.vetclinic.service.MedicalRecordPdfService;
import com.softart.vetclinic.service.MedicalRecordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/medical-records")
@RequiredArgsConstructor
public class MedicalRecordController {

    private final MedicalRecordService medicalRecordService;
    private final MedicalRecordMapper medicalRecordMapper;
    private final MedicalRecordPdfService medicalRecordPdfService;

   
    private final PetRepository petRepository;
    private final UserRepository userRepository;


    @GetMapping("/{id}")
    public MedicalRecordResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return medicalRecordMapper.toResponse(medicalRecordService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicalRecordResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateMedicalRecordRequest request) {
        var entity = medicalRecordMapper.toEntity(request);
        return medicalRecordMapper.toResponse(medicalRecordService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public MedicalRecordResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateMedicalRecordRequest request) {
        return medicalRecordMapper.toResponse(
                medicalRecordService.update(id, clinicId, existing -> medicalRecordMapper.updateEntity(request, existing)));
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
        return medicalRecordService.findByPet(clinicId, petId).stream()
                .map(medicalRecordMapper::toResponse).toList();
    }

    @GetMapping("/by-appointment/{appointmentId}")
    public MedicalRecordResponse getByAppointment(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID appointmentId) {
        return medicalRecordService.findByAppointment(appointmentId)
                .map(medicalRecordMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("MedicalRecord", "appointmentId", appointmentId));
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

        Set<UUID> petIds = records.stream().map(MedicalRecord::getPetId).collect(Collectors.toSet());
        Map<UUID, String> petNames = petRepository.findAllById(petIds).stream()
                .collect(Collectors.toMap(p -> p.getId(), p -> p.getName()));

        Set<UUID> vetIds = records.stream().map(MedicalRecord::getVetId).collect(Collectors.toSet());
        Map<UUID, String> vetNames = userRepository.findAllById(vetIds).stream()
                .collect(Collectors.toMap(u -> u.getId(), u -> u.getFirstName() + " " + u.getLastName()));

        return records.stream().map(r -> new MedicalRecordResponse(
                r.getId(),
                r.getAppointmentId(),
                r.getPetId(),
                petNames.getOrDefault(r.getPetId(), ""),
                r.getVetId(),
                vetNames.getOrDefault(r.getVetId(), ""),
                r.getSymptoms(),
                r.getDiagnosis(),
                r.getExaminationNotes(),
                r.getWeightKg(),
                r.getTemperatureC(),
                r.getHeartRate(),
                r.getFollowUpRecommended(),
                r.getFollowUpDate(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        )).toList();
    }
    
    @GetMapping
    public Page<MedicalRecordResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(required = false) String search,
            Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "createdAt"));
        }
        return medicalRecordService.searchAll(clinicId, search, pageable).map(medicalRecordMapper::toResponse);
    }

}
