package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateProductRequest;
import com.softart.vetclinic.dto.ProductResponse;
import com.softart.vetclinic.dto.UpdateProductRequest;
import com.softart.vetclinic.entity.Product;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface ProductMapper {

    ProductResponse toResponse(Product entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    Product toEntity(CreateProductRequest dto);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "clinicId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deleted", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "version", ignore = true)
    @Mapping(target = "clinic", ignore = true)
    void updateEntity(UpdateProductRequest dto, @MappingTarget Product entity);
}