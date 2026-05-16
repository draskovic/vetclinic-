package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateInventoryItemRequest;
import com.softart.vetclinic.dto.InventoryItemResponse;
import com.softart.vetclinic.dto.UpdateInventoryItemRequest;
import com.softart.vetclinic.entity.InventoryItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface InventoryItemMapper {

	@Mapping(target = "locationName", ignore = true)
	@Mapping(target = "taxRateLabel", ignore = true)
    @Mapping(target = "taxRatePercent", ignore = true)
	InventoryItemResponse toResponse(InventoryItem entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "location", ignore = true)
    InventoryItem toEntity(CreateInventoryItemRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "location", ignore = true)
    void updateEntity(UpdateInventoryItemRequest dto, @MappingTarget InventoryItem entity);
}
