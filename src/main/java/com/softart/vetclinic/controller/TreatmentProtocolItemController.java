package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateTreatmentProtocolItemRequest;
import com.softart.vetclinic.dto.TreatmentProtocolItemResponse;
import com.softart.vetclinic.dto.UpdateTreatmentProtocolItemRequest;
import com.softart.vetclinic.mapper.TreatmentProtocolItemMapper;
import com.softart.vetclinic.repository.ServiceRepository;
import com.softart.vetclinic.service.TreatmentProtocolItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/treatment-protocol-items")
@RequiredArgsConstructor
public class TreatmentProtocolItemController {

    private final TreatmentProtocolItemService itemService;
    private final TreatmentProtocolItemMapper itemMapper;
    private final ServiceRepository serviceRepository;

    @GetMapping("/by-protocol/{protocolId}")
    public List<TreatmentProtocolItemResponse> getByProtocol(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID protocolId) {
        var items = itemService.findByProtocol(clinicId, protocolId);

        // Batch-fetch service details
        var serviceIds = items.stream()
                .map(i -> i.getServiceId())
                .distinct()
                .toList();
        Map<UUID, com.softart.vetclinic.entity.Service> serviceMap = serviceIds.stream()
                .map(sid -> serviceRepository.findByIdAndClinicIdAndDeletedFalse(sid, clinicId).orElse(null))
                .filter(s -> s != null)
                .collect(Collectors.toMap(s -> s.getId(), s -> s));

        return items.stream().map(item -> {
            var response = itemMapper.toResponse(item);
            var service = serviceMap.get(item.getServiceId());
            if (service != null) {
                return new TreatmentProtocolItemResponse(
                    response.id(), response.protocolId(), response.serviceId(),
                    service.getName(), service.getSku(), service.getPrice(),
                    response.quantity(), response.notes(), response.sortOrder(),
                    response.createdAt(), response.updatedAt()
                );
            }
            return response;
        }).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TreatmentProtocolItemResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateTreatmentProtocolItemRequest request) {
        var entity = itemMapper.toEntity(request);
        var result = itemService.create(entity, clinicId);

        // Dohvati service detalje za response
        var response = itemMapper.toResponse(result);
        return serviceRepository.findByIdAndClinicIdAndDeletedFalse(result.getServiceId(), clinicId)
                .map(service -> new TreatmentProtocolItemResponse(
                    response.id(), response.protocolId(), response.serviceId(),
                    service.getName(), service.getSku(), service.getPrice(),
                    response.quantity(), response.notes(), response.sortOrder(),
                    response.createdAt(), response.updatedAt()
                ))
                .orElse(response);
    }

    @PutMapping("/{id}")
    public TreatmentProtocolItemResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateTreatmentProtocolItemRequest request) {
        var result = itemService.update(id, clinicId,
                existing -> itemMapper.updateEntity(request, existing));

        var response = itemMapper.toResponse(result);
        return serviceRepository.findByIdAndClinicIdAndDeletedFalse(result.getServiceId(), clinicId)
                .map(service -> new TreatmentProtocolItemResponse(
                    response.id(), response.protocolId(), response.serviceId(),
                    service.getName(), service.getSku(), service.getPrice(),
                    response.quantity(), response.notes(), response.sortOrder(),
                    response.createdAt(), response.updatedAt()
                ))
                .orElse(response);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        itemService.softDelete(id, clinicId);
    }
}
