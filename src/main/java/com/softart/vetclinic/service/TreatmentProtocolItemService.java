package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.TreatmentProtocolItem;
import com.softart.vetclinic.repository.TreatmentProtocolItemRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@org.springframework.stereotype.Service
public class TreatmentProtocolItemService extends AbstractCrudService<TreatmentProtocolItem, TreatmentProtocolItemRepository> {

    private final TreatmentProtocolItemRepository itemRepository;

    public TreatmentProtocolItemService(TreatmentProtocolItemRepository itemRepository) {
        super(itemRepository);
        this.itemRepository = itemRepository;
    }

    @Override
    protected String getEntityName() {
        return "TreatmentProtocolItem";
    }

    @Override
    protected Optional<TreatmentProtocolItem> findByIdAndClinicId(UUID id, UUID clinicId) {
        return itemRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<TreatmentProtocolItem> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return itemRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return itemRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(TreatmentProtocolItem entity) {
        // Nema posebne validacije — protocolId i serviceId FK constraint-i su dovoljni
    }

    @Transactional(readOnly = true)
    public List<TreatmentProtocolItem> findByProtocol(UUID clinicId, UUID protocolId) {
        return itemRepository.findByClinicIdAndProtocolIdAndDeletedFalseOrderBySortOrderAsc(clinicId, protocolId);
    }
}
