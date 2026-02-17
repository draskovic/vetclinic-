package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.AppointmentResponse;
import com.softart.vetclinic.dto.CreateAppointmentRequest;
import com.softart.vetclinic.dto.UpdateAppointmentRequest;
import com.softart.vetclinic.entity.Appointment;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AppointmentMapper {

    @Mapping(target = "locationName", source = "location.name")
    @Mapping(target = "petName", source = "pet.name")
    @Mapping(target = "ownerName", expression = "java(entity.getOwner() != null ? entity.getOwner().getFirstName() + \" \" + entity.getOwner().getLastName() : null)")
    @Mapping(target = "vetName", expression = "java(entity.getVet() != null ? entity.getVet().getFirstName() + \" \" + entity.getVet().getLastName() : null)")
    AppointmentResponse toResponse(Appointment entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "vet", ignore = true)
    @Mapping(target = "followUpAppointment", ignore = true)
    Appointment toEntity(CreateAppointmentRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "vet", ignore = true)
    @Mapping(target = "followUpAppointment", ignore = true)
    void updateEntity(UpdateAppointmentRequest dto, @MappingTarget Appointment entity);
}
