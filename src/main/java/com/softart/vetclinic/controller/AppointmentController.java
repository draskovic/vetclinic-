package com.softart.vetclinic.controller;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
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

import com.softart.vetclinic.config.tenant.ClinicContextHolder;
import com.softart.vetclinic.dto.AppointmentResponse;
import com.softart.vetclinic.dto.CreateAppointmentRequest;
import com.softart.vetclinic.dto.UpdateAppointmentRequest;
import com.softart.vetclinic.entity.Appointment;
import com.softart.vetclinic.enums.AppointmentStatus;
import com.softart.vetclinic.exception.BadRequestException;
import com.softart.vetclinic.mapper.AppointmentMapper;
import com.softart.vetclinic.repository.AppointmentRepository;
import com.softart.vetclinic.service.AppointmentService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentMapper appointmentMapper;
    private final AppointmentRepository appointmentRepository;

    @GetMapping
    public Page<AppointmentResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AppointmentStatus status,
            Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Direction.DESC, "startTime"));
        }
        return appointmentService.searchAll(clinicId, search, status, pageable).map(appointmentMapper::toResponse);
    }


    @GetMapping("/{id}")
    public AppointmentResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return appointmentMapper.toResponse(appointmentService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateAppointmentRequest request) {
        var entity = appointmentMapper.toEntity(request);
        return appointmentMapper.toResponse(appointmentService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public AppointmentResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAppointmentRequest request) {
        return appointmentMapper.toResponse(
                appointmentService.update(id, clinicId, existing -> appointmentMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        appointmentService.softDelete(id, clinicId);
    }

    @GetMapping("/date-range")
    public List<AppointmentResponse> getByDateRange(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return appointmentService.findByDateRange(clinicId, from, to).stream()
                .map(appointmentMapper::toResponse).toList();
    }

    @GetMapping("/by-vet/{vetId}")
    public List<AppointmentResponse> getByVet(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID vetId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime to) {
        return appointmentService.findByVetAndDateRange(clinicId, vetId, from, to).stream()
                .map(appointmentMapper::toResponse).toList();
    }
    
    @GetMapping("/by-pet/{petId}")                                                                                            
    public List<AppointmentResponse> getByPet(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID petId) {
        return appointmentService.findByPet(clinicId, petId)
                .stream()
                .map(appointmentMapper::toResponse)
                .toList();
    }
    
    @GetMapping("/by-owner/{ownerId}")
    public List<AppointmentResponse> getByOwner(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID ownerId) {
        return appointmentService.findByOwner(clinicId, ownerId).stream()
                .map(appointmentMapper::toResponse).toList();
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<AppointmentResponse> approve(@PathVariable UUID id) {
        UUID clinicId = ClinicContextHolder.get();
        Appointment saved = appointmentService.update(id, clinicId, a -> {
            if (a.getStatus() != AppointmentStatus.PENDING) {
                throw new BadRequestException("Samo termini sa statusom 'Na čekanju' mogu biti odobreni");
            }
            a.setStatus(AppointmentStatus.CONFIRMED);
        });
        return ResponseEntity.ok(appointmentMapper.toResponse(saved));
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> reject(@PathVariable UUID id) {
        UUID clinicId = ClinicContextHolder.get();
        appointmentService.update(id, clinicId, a -> {
            if (a.getStatus() != AppointmentStatus.PENDING) {
                throw new BadRequestException("Samo termini sa statusom 'Na čekanju' mogu biti odbijeni");
            }
            a.setStatus(AppointmentStatus.CANCELLED);
        });
        return ResponseEntity.ok().build();
    }

    @GetMapping("/count-pending")
    public ResponseEntity<Long> countPending() {
        UUID clinicId = ClinicContextHolder.get();
        long count = appointmentRepository.countByClinicIdAndDeletedFalseAndStatus(clinicId, AppointmentStatus.PENDING);
        return ResponseEntity.ok(count);
    }

}
