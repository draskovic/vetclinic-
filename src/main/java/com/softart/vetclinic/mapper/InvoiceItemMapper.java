package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateInvoiceItemRequest;
import com.softart.vetclinic.dto.InvoiceItemResponse;
import com.softart.vetclinic.dto.UpdateInvoiceItemRequest;
import com.softart.vetclinic.entity.InvoiceItem;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface InvoiceItemMapper {

    @Mapping(target = "serviceName", source = "service.name")
    InvoiceItemResponse toResponse(InvoiceItem entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "service", ignore = true)
    @Mapping(target = "taxRateLabel", ignore = true)
    @Mapping(target = "taxRatePercent", ignore = true)
    InvoiceItem toEntity(CreateInvoiceItemRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    @Mapping(target = "invoice", ignore = true)
    @Mapping(target = "service", ignore = true)
    @Mapping(target = "taxRateLabel", ignore = true)
    @Mapping(target = "taxRatePercent", ignore = true)
    void updateEntity(UpdateInvoiceItemRequest dto, @MappingTarget InvoiceItem entity);
}