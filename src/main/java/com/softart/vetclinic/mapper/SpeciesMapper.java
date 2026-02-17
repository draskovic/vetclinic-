package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateSpeciesRequest;
import com.softart.vetclinic.dto.SpeciesResponse;
import com.softart.vetclinic.dto.UpdateSpeciesRequest;
import com.softart.vetclinic.entity.Species;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface SpeciesMapper {

    SpeciesResponse toResponse(Species entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    Species toEntity(CreateSpeciesRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    void updateEntity(UpdateSpeciesRequest dto, @MappingTarget Species entity);
}
