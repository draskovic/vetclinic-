package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateInventoryTransactionRequest;
import com.softart.vetclinic.dto.InventoryTransactionResponse;
import com.softart.vetclinic.dto.UpdateInventoryTransactionRequest;
import com.softart.vetclinic.entity.InventoryTransaction;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface InventoryTransactionMapper {

    @Mapping(target = "inventoryItemName", ignore = true)
	@Mapping(target = "performedByName", expression = "java(entity.getPerformedByUser() != null ? entity.getPerformedByUser().getFirstName() + \" \" + entity.getPerformedByUser().getLastName() : null)")
	@Mapping(target = "batchDeleted", ignore = true)    // ← NOVO
	@Mapping(target = "batchNumber", ignore = true)
    InventoryTransactionResponse toResponse(InventoryTransaction entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "inventoryItem", ignore = true)
    @Mapping(target = "performedByUser", ignore = true)
    @Mapping(target = "batchId", ignore = true)
    @Mapping(target = "reversalOfTransactionId", ignore = true)
    @Mapping(target = "reversed", ignore = true)
    InventoryTransaction toEntity(CreateInventoryTransactionRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "inventoryItem", ignore = true)
    @Mapping(target = "performedByUser", ignore = true)
    @Mapping(target = "batchId", ignore = true)
    @Mapping(target = "reversalOfTransactionId", ignore = true)
    @Mapping(target = "reversed", ignore = true)
    void updateEntity(UpdateInventoryTransactionRequest dto, @MappingTarget InventoryTransaction entity);
}
