package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateMedicalRecordRequest;
import com.softart.vetclinic.dto.MedicalRecordResponse;
import com.softart.vetclinic.dto.UpdateMedicalRecordRequest;
import com.softart.vetclinic.entity.MedicalRecord;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface MedicalRecordMapper {

    @Mapping(target = "petName", source = "pet.name")
    @Mapping(target = "vetName", expression = "java(entity.getVet() != null ? entity.getVet().getFirstName() + \" \" + entity.getVet().getLastName() : null)")
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "ownerName", ignore = true)
    MedicalRecordResponse toResponse(MedicalRecord entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "appointment", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "vet", ignore = true)
    MedicalRecord toEntity(CreateMedicalRecordRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "appointmentId", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "appointment", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "vet", ignore = true)
    void updateEntity(UpdateMedicalRecordRequest dto, @MappingTarget MedicalRecord entity);
}
