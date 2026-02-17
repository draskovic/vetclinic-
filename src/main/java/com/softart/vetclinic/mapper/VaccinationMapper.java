package com.softart.vetclinic.mapper;

import com.softart.vetclinic.dto.CreateVaccinationRequest;
import com.softart.vetclinic.dto.UpdateVaccinationRequest;
import com.softart.vetclinic.dto.VaccinationResponse;
import com.softart.vetclinic.entity.Vaccination;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface VaccinationMapper {

    @Mapping(target = "petName", source = "pet.name")
    @Mapping(target = "vetName", expression = "java(entity.getVet() != null ? entity.getVet().getFirstName() + \" \" + entity.getVet().getLastName() : null)")
    VaccinationResponse toResponse(Vaccination entity);

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
    @Mapping(target = "vet", ignore = true)
    Vaccination toEntity(CreateVaccinationRequest dto);

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
    @Mapping(target = "vet", ignore = true)
    void updateEntity(UpdateVaccinationRequest dto, @MappingTarget Vaccination entity);
}
