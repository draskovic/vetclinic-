package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.ClinicLocationResponse;
import com.softart.vetclinic.dto.CreateClinicLocationRequest;
import com.softart.vetclinic.dto.UpdateClinicLocationRequest;
import com.softart.vetclinic.entity.ClinicLocation;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ClinicLocationMapper {

    ClinicLocationResponse toResponse(ClinicLocation entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    ClinicLocation toEntity(CreateClinicLocationRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    void updateEntity(UpdateClinicLocationRequest dto, @MappingTarget ClinicLocation entity);
}
