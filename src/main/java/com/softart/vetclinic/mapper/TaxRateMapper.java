package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.TaxRateResponse;
import com.softart.vetclinic.entity.TaxRate;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TaxRateMapper {
    TaxRateResponse toResponse(TaxRate entity);
}