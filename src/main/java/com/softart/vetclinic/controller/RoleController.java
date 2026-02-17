package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateRoleRequest;
import com.softart.vetclinic.dto.RoleResponse;
import com.softart.vetclinic.dto.UpdateRoleRequest;
import com.softart.vetclinic.mapper.RoleMapper;
import com.softart.vetclinic.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;
    private final RoleMapper roleMapper;

    @GetMapping
    public Page<RoleResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            Pageable pageable) {
        return roleService.findAll(clinicId, pageable).map(roleMapper::toResponse);
    }

    @GetMapping("/{id}")
    public RoleResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return roleMapper.toResponse(roleService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RoleResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateRoleRequest request) {
        var entity = roleMapper.toEntity(request);
        return roleMapper.toResponse(roleService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public RoleResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        return roleMapper.toResponse(
                roleService.update(id, clinicId, existing -> roleMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        roleService.softDelete(id, clinicId);
    }
}
