package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateServiceInventoryItemRequest;
import com.softart.vetclinic.dto.ServiceInventoryItemResponse;
import com.softart.vetclinic.dto.UpdateServiceInventoryItemRequest;
import com.softart.vetclinic.entity.ServiceInventoryItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ServiceInventoryItemMapper {

    @Mapping(target = "serviceName", ignore = true)
    @Mapping(target = "productName", ignore = true)
    @Mapping(target = "unit", ignore = true)
    ServiceInventoryItemResponse toResponse(ServiceInventoryItem entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "service", ignore = true)
    @Mapping(target = "product", ignore = true)
    ServiceInventoryItem toEntity(CreateServiceInventoryItemRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "service", ignore = true)
    @Mapping(target = "product", ignore = true)
    @Mapping(target = "serviceId", ignore = true)
    @Mapping(target = "productId", ignore = true)
    void updateEntity(UpdateServiceInventoryItemRequest dto, @MappingTarget ServiceInventoryItem entity);
}