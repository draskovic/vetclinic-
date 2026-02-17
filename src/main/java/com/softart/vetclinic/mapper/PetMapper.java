package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreatePetRequest;
import com.softart.vetclinic.dto.PetResponse;
import com.softart.vetclinic.dto.UpdatePetRequest;
import com.softart.vetclinic.entity.Pet;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface PetMapper {

    @Mapping(target = "ownerName", expression = "java(entity.getOwner() != null ? entity.getOwner().getFirstName() + \" \" + entity.getOwner().getLastName() : null)")
    @Mapping(target = "speciesName", source = "species.name")
    @Mapping(target = "breedName", source = "breed.name")
    PetResponse toResponse(Pet entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "species", ignore = true)
    @Mapping(target = "breed", ignore = true)
    Pet toEntity(CreatePetRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "species", ignore = true)
    @Mapping(target = "breed", ignore = true)
    void updateEntity(UpdatePetRequest dto, @MappingTarget Pet entity);
}
