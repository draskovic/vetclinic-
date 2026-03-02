package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateNotificationRequest;
import com.softart.vetclinic.dto.NotificationResponse;
import com.softart.vetclinic.dto.UpdateNotificationRequest;
import com.softart.vetclinic.entity.Notification;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    Notification toEntity(CreateNotificationRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "failureReason", ignore = true)
    @Mapping(target = "readAt", ignore = true)
    void updateEntity(UpdateNotificationRequest dto, @MappingTarget Notification entity);
}
