package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreatePrescriptionRequest;
import com.softart.vetclinic.dto.PrescriptionResponse;
import com.softart.vetclinic.dto.UpdatePrescriptionRequest;
import com.softart.vetclinic.entity.Prescription;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PrescriptionMapper {

    @Mapping(target = "petName", source = "pet.name")
    @Mapping(target = "vetName", expression = "java(entity.getVet() != null ? entity.getVet().getFirstName() + \" \" + entity.getVet().getLastName() : null)")
    @Mapping(target = "inventoryItemName", source = "inventoryItem.product.name")
    PrescriptionResponse toResponse(Prescription entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "medicalRecord", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "vet", ignore = true)
    @Mapping(target = "inventoryItem", ignore = true)
    Prescription toEntity(CreatePrescriptionRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "medicalRecord", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "vet", ignore = true)
    @Mapping(target = "inventoryItem", ignore = true)
    void updateEntity(UpdatePrescriptionRequest dto, @MappingTarget Prescription entity);
}
