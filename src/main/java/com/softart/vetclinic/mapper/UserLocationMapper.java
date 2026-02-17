package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateUserLocationRequest;
import com.softart.vetclinic.dto.UpdateUserLocationRequest;
import com.softart.vetclinic.dto.UserLocationResponse;
import com.softart.vetclinic.entity.UserLocation;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface UserLocationMapper {

    @Mapping(target = "userName", expression = "java(entity.getUser() != null ? entity.getUser().getFirstName() + \" \" + entity.getUser().getLastName() : null)")
    @Mapping(target = "locationName", source = "location.name")
    UserLocationResponse toResponse(UserLocation entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "location", ignore = true)
    UserLocation toEntity(CreateUserLocationRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "location", ignore = true)
    void updateEntity(UpdateUserLocationRequest dto, @MappingTarget UserLocation entity);
}
