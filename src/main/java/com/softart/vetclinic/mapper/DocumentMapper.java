package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateDocumentRequest;
import com.softart.vetclinic.dto.DocumentResponse;
import com.softart.vetclinic.dto.UpdateDocumentRequest;
import com.softart.vetclinic.entity.Document;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface DocumentMapper {

    @Mapping(target = "petName", source = "pet.name")
    @Mapping(target = "uploadedByName", expression = "java(entity.getUploadedByUser() != null ? entity.getUploadedByUser().getFirstName() + \" \" + entity.getUploadedByUser().getLastName() : null)")
    DocumentResponse toResponse(Document entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "pet", ignore = true)
    @Mapping(target = "medicalRecord", ignore = true)
    @Mapping(target = "uploadedByUser", ignore = true)
    Document toEntity(CreateDocumentRequest dto);

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
    @Mapping(target = "medicalRecord", ignore = true)
    @Mapping(target = "uploadedByUser", ignore = true)
    void updateEntity(UpdateDocumentRequest dto, @MappingTarget Document entity);
}
