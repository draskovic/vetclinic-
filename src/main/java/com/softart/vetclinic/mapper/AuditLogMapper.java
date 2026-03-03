package com.softart.vetclinic.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.softart.vetclinic.dto.AuditLogResponse;
import com.softart.vetclinic.entity.AuditLog;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    @Mapping(target = "userName", ignore = true)
    AuditLogResponse toResponse(AuditLog entity);
}

