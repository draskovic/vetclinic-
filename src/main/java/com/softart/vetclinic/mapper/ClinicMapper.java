package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.ClinicResponse;
import com.softart.vetclinic.dto.CreateClinicRequest;
import com.softart.vetclinic.dto.UpdateClinicRequest;
import com.softart.vetclinic.entity.Clinic;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ClinicMapper {

    ClinicResponse toResponse(Clinic entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    Clinic toEntity(CreateClinicRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    void updateEntity(UpdateClinicRequest dto, @MappingTarget Clinic entity);
}
