package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateMedicationAdministrationRequest;
import com.softart.vetclinic.dto.MedicationAdministrationResponse;
import com.softart.vetclinic.dto.UpdateMedicationAdministrationRequest;
import com.softart.vetclinic.entity.MedicationAdministration;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface MedicationAdministrationMapper {

    @Mapping(target = "petName", source = "pet.name")
    @Mapping(target = "vetName", expression = "java(entity.getVet() != null ? entity.getVet().getFirstName() + \" \" + entity.getVet().getLastName() : null)")
    @Mapping(target = "inventoryItemName", source = "inventoryItem.name")
    MedicationAdministrationResponse toResponse(MedicationAdministration entity);

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
    MedicationAdministration toEntity(CreateMedicationAdministrationRequest dto);

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
    @Mapping(target = "inventoryItemId", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "dosage", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "route", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "instructions", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void updateEntity(UpdateMedicationAdministrationRequest dto, @MappingTarget MedicationAdministration entity);
}