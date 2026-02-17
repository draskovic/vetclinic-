package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateUserLocationRequest;
import com.softart.vetclinic.dto.UpdateUserLocationRequest;
import com.softart.vetclinic.dto.UserLocationResponse;
import com.softart.vetclinic.mapper.UserLocationMapper;
import com.softart.vetclinic.service.UserLocationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/user-locations")
@RequiredArgsConstructor
public class UserLocationController {

    private final UserLocationService userLocationService;
    private final UserLocationMapper userLocationMapper;

    @GetMapping
    public Page<UserLocationResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return userLocationService.findAll(clinicId, pageable).map(userLocationMapper::toResponse);
    }

    @GetMapping("/{id}")
    public UserLocationResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return userLocationMapper.toResponse(userLocationService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserLocationResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateUserLocationRequest request) {
        var entity = userLocationMapper.toEntity(request);
        return userLocationMapper.toResponse(userLocationService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public UserLocationResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateUserLocationRequest request) {
        return userLocationMapper.toResponse(
                userLocationService.update(id, clinicId, existing -> userLocationMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        userLocationService.softDelete(id, clinicId);
    }

    @GetMapping("/by-user/{userId}")
    public List<UserLocationResponse> getByUser(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID userId) {
        return userLocationService.findByUser(clinicId, userId).stream()
                .map(userLocationMapper::toResponse).toList();
    }
}
