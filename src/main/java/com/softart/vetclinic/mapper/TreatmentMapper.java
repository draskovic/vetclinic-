package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateTreatmentRequest;
import com.softart.vetclinic.dto.TreatmentResponse;
import com.softart.vetclinic.dto.UpdateTreatmentRequest;
import com.softart.vetclinic.entity.Treatment;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TreatmentMapper {

    @Mapping(target = "serviceName", source = "service.name")
    @Mapping(target = "vetName", expression = "java(entity.getVet() != null ? entity.getVet().getFirstName() + \" \" + entity.getVet().getLastName() : null)")
    TreatmentResponse toResponse(Treatment entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "medicalRecord", ignore = true)
    @Mapping(target = "service", ignore = true)
    @Mapping(target = "vet", ignore = true)
    Treatment toEntity(CreateTreatmentRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "medicalRecord", ignore = true)
    @Mapping(target = "service", ignore = true)
    @Mapping(target = "vet", ignore = true)
    void updateEntity(UpdateTreatmentRequest dto, @MappingTarget Treatment entity);
}
