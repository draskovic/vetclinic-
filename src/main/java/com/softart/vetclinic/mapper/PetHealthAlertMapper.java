package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreatePetHealthAlertRequest;
import com.softart.vetclinic.dto.PetHealthAlertResponse;
import com.softart.vetclinic.dto.UpdatePetHealthAlertRequest;
import com.softart.vetclinic.entity.PetHealthAlert;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PetHealthAlertMapper {

    @Mapping(target = "petName", source = "pet.name")
    PetHealthAlertResponse toResponse(PetHealthAlert entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "pet", ignore = true)
    PetHealthAlert toEntity(CreatePetHealthAlertRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "petId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "description", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void updateEntity(UpdatePetHealthAlertRequest dto, @MappingTarget PetHealthAlert entity);
}