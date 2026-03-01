package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateLabReportRequest;
import com.softart.vetclinic.dto.LabReportResponse;
import com.softart.vetclinic.dto.UpdateLabReportRequest;
import com.softart.vetclinic.entity.LabReport;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface LabReportMapper {

    @Mapping(target = "petName", source = "pet.name")
    @Mapping(target = "ownerName", expression = "java(entity.getPet() != null && entity.getPet().getOwner() != null ? entity.getPet().getOwner().getFirstName() + \" \" + entity.getPet().getOwner().getLastName() : null)")
    @Mapping(target = "vetName", expression = "java(entity.getVet() != null ? entity.getVet().getFirstName() + \" \" + entity.getVet().getLastName() : null)")
    LabReportResponse toResponse(LabReport entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "vet", ignore = true)
    @Mapping(target = "medicalRecord", ignore = true)
    @Mapping(target = "fileName", ignore = true)
    @Mapping(target = "mimeType", ignore = true)
    @Mapping(target = "fileSizeBytes", ignore = true)
    @Mapping(target = "storagePath", ignore = true)
    @Mapping(target = "isAbnormal", defaultExpression = "java(false)")
    @Mapping(target = "status", defaultExpression = "java(com.softart.vetclinic.enums.LabReportStatus.PENDING)")
    @Mapping(target = "testCategory", defaultExpression = "java(com.softart.vetclinic.enums.TestCategory.LABORATORY)")
    LabReport toEntity(CreateLabReportRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "vet", ignore = true)
    @Mapping(target = "medicalRecord", ignore = true)
    @Mapping(target = "fileName", ignore = true)
    @Mapping(target = "mimeType", ignore = true)
    @Mapping(target = "fileSizeBytes", ignore = true)
    @Mapping(target = "storagePath", ignore = true)
    @Mapping(target = "medicalRecordId", nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.SET_TO_NULL)
    void updateEntity(UpdateLabReportRequest dto, @MappingTarget LabReport entity);
}
