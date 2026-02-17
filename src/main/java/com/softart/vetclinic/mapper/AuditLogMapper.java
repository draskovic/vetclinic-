package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.AuditLogResponse;
import com.softart.vetclinic.entity.AuditLog;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "clinicId", source = "clinicId")
    @Mapping(target = "userId", source = "userId")
    AuditLogResponse toResponse(AuditLog entity);
}
