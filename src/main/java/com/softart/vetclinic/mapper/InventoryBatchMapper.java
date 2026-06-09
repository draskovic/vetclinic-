package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateInventoryBatchRequest;
import com.softart.vetclinic.dto.InventoryBatchResponse;
import com.softart.vetclinic.dto.UpdateInventoryBatchRequest;
import com.softart.vetclinic.entity.InventoryBatch;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface InventoryBatchMapper {

    @Mapping(target = "inventoryItemName", ignore = true)
    @Mapping(target = "inventoryItemUnit", ignore = true)
    @Mapping(target = "daysUntilExpiry", ignore = true)
    @Mapping(target = "status", ignore = true)
    InventoryBatchResponse toResponse(InventoryBatch entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "inventoryItem", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    InventoryBatch toEntity(CreateInventoryBatchRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "inventoryItemId", ignore = true)
    @Mapping(target = "quantityOnHand", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "inventoryItem", ignore = true)
    @Mapping(target = "isDefault", ignore = true)
    void updateEntity(UpdateInventoryBatchRequest dto, @MappingTarget InventoryBatch entity);
}
