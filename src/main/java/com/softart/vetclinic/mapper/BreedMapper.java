package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.BreedResponse;
import com.softart.vetclinic.dto.CreateBreedRequest;
import com.softart.vetclinic.dto.UpdateBreedRequest;
import com.softart.vetclinic.entity.Breed;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface BreedMapper {

    @Mapping(target = "speciesName", source = "species.name")
    BreedResponse toResponse(Breed entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "species", ignore = true)
    Breed toEntity(CreateBreedRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "species", ignore = true)
    void updateEntity(UpdateBreedRequest dto, @MappingTarget Breed entity);
}
