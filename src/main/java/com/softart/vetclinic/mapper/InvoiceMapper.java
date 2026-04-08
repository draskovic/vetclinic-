package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateInvoiceRequest;
import com.softart.vetclinic.dto.InvoiceResponse;
import com.softart.vetclinic.dto.UpdateInvoiceRequest;
import com.softart.vetclinic.entity.Invoice;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface InvoiceMapper {

    @Mapping(target = "ownerName", expression = "java(entity.getOwner() != null ? entity.getOwner().getFirstName() + \" \" + entity.getOwner().getLastName() : null)")
    @Mapping(target = "locationName", source = "location.name")
    InvoiceResponse toResponse(Invoice entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "appointment", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "location", ignore = true)
    Invoice toEntity(CreateInvoiceRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "invoiceNumber", ignore = true)
    @Mapping(target = "appointment", ignore = true)
    @Mapping(target = "owner", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "medicalRecordId", ignore = true)
    void updateEntity(UpdateInvoiceRequest dto, @MappingTarget Invoice entity);
}
