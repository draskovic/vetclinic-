package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateTreatmentProtocolRequest;
import com.softart.vetclinic.dto.TreatmentProtocolResponse;
import com.softart.vetclinic.dto.UpdateTreatmentProtocolRequest;
import com.softart.vetclinic.entity.TreatmentProtocol;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TreatmentProtocolMapper {

    @Mapping(target = "diagnosisName", ignore = true)
    TreatmentProtocolResponse toResponse(TreatmentProtocol entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "diagnosis", ignore = true)
    TreatmentProtocol toEntity(CreateTreatmentProtocolRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "diagnosis", ignore = true)
    void updateEntity(UpdateTreatmentProtocolRequest dto, @MappingTarget TreatmentProtocol entity);
}
