package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateDiagnosisRequest;
import com.softart.vetclinic.dto.DiagnosisResponse;
import com.softart.vetclinic.dto.UpdateDiagnosisRequest;
import com.softart.vetclinic.entity.Diagnosis;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DiagnosisMapper {

    DiagnosisResponse toResponse(Diagnosis entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    Diagnosis toEntity(CreateDiagnosisRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    void updateEntity(UpdateDiagnosisRequest dto, @MappingTarget Diagnosis entity);
}
