package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.TreatmentProtocolItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TreatmentProtocolItemRepository extends JpaRepository<TreatmentProtocolItem, UUID> {

    Optional<TreatmentProtocolItem> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    Page<TreatmentProtocolItem> findByClinicIdAndDeletedFalse(UUID clinicId, Pageable pageable);

    boolean existsByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    List<TreatmentProtocolItem> findByClinicIdAndProtocolIdAndDeletedFalseOrderBySortOrderAsc(UUID clinicId, UUID protocolId);
}
