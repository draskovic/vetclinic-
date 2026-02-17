package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.AppointmentResponse;
import com.softart.vetclinic.dto.CreateAppointmentRequest;
import com.softart.vetclinic.dto.UpdateAppointmentRequest;
import com.softart.vetclinic.mapper.AppointmentMapper;
import com.softart.vetclinic.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AppointmentMapper appointmentMapper;

    @GetMapping
    public Page<AppointmentResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return appointmentService.findAll(clinicId, pageable).map(appointmentMapper::toResponse);
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
}
