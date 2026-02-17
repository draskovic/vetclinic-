package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateServiceRequest;
import com.softart.vetclinic.dto.ServiceResponse;
import com.softart.vetclinic.dto.UpdateServiceRequest;
import com.softart.vetclinic.entity.Service;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ServiceMapper {

    ServiceResponse toResponse(Service entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    Service toEntity(CreateServiceRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    void updateEntity(UpdateServiceRequest dto, @MappingTarget Service entity);
}
