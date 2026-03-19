package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.ClinicResponse;
import com.softart.vetclinic.dto.ProvisionClinicRequest;
import com.softart.vetclinic.dto.ProvisionClinicResponse;
import com.softart.vetclinic.service.ClinicProvisioningService;

import com.softart.vetclinic.dto.CreateClinicRequest;
import com.softart.vetclinic.dto.UpdateClinicRequest;
import com.softart.vetclinic.mapper.ClinicMapper;
import com.softart.vetclinic.service.ClinicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/clinics")
@RequiredArgsConstructor
public class ClinicController {

    private final ClinicService clinicService;
    private final ClinicMapper clinicMapper;
    private final ClinicProvisioningService clinicProvisioningService;


    @GetMapping
    public Page<ClinicResponse> getAll(Pageable pageable) {
        return clinicService.findAll(pageable).map(clinicMapper::toResponse);
    }

    @GetMapping("/{id}")
    public ClinicResponse getById(@PathVariable UUID id) {
        return clinicMapper.toResponse(clinicService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicResponse create(@Valid @RequestBody CreateClinicRequest request) {
        var entity = clinicMapper.toEntity(request);
        return clinicMapper.toResponse(clinicService.create(entity));
    }

    @PutMapping("/{id}")
    public ClinicResponse update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateClinicRequest request) {
        return clinicMapper.toResponse(
                clinicService.update(id, existing -> clinicMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        clinicService.softDelete(id);
    }
    
    @PostMapping("/provision")
    @ResponseStatus(HttpStatus.CREATED)
    public ProvisionClinicResponse provision(@Valid @RequestBody ProvisionClinicRequest request) {
        return clinicProvisioningService.provision(request);
    }

    @GetMapping("/lookup")
    public ClinicResponse getByEmail(@RequestParam String email) {
        return clinicMapper.toResponse(clinicService.findByEmail(email));
    }

}
