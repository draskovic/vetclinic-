package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateTreatmentProtocolItemRequest;
import com.softart.vetclinic.dto.TreatmentProtocolItemResponse;
import com.softart.vetclinic.dto.UpdateTreatmentProtocolItemRequest;
import com.softart.vetclinic.entity.TreatmentProtocolItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface TreatmentProtocolItemMapper {

    @Mapping(target = "serviceName", ignore = true)
    @Mapping(target = "serviceSku", ignore = true)
    @Mapping(target = "servicePrice", ignore = true)
    TreatmentProtocolItemResponse toResponse(TreatmentProtocolItem entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "protocol", ignore = true)
    @Mapping(target = "service", ignore = true)
    TreatmentProtocolItem toEntity(CreateTreatmentProtocolItemRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "protocol", ignore = true)
    @Mapping(target = "service", ignore = true)
    void updateEntity(UpdateTreatmentProtocolItemRequest dto, @MappingTarget TreatmentProtocolItem entity);
}
