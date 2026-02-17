package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateOwnerRequest;
import com.softart.vetclinic.dto.OwnerResponse;
import com.softart.vetclinic.dto.UpdateOwnerRequest;
import com.softart.vetclinic.entity.Owner;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface OwnerMapper {

    OwnerResponse toResponse(Owner entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    Owner toEntity(CreateOwnerRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    void updateEntity(UpdateOwnerRequest dto, @MappingTarget Owner entity);
}
