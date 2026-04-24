package com.softart.vetclinic.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.softart.vetclinic.entity.InventoryBatch;

import jakarta.persistence.LockModeType;

@Repository
public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, UUID> {

    // Pojedinačan lot (sa proverom klinike i soft delete)
    Optional<InventoryBatch> findByIdAndClinicIdAndDeletedFalse(UUID id, UUID clinicId);

    // Svi lotovi za artikal (za tab "Lotovi" u UI), sortirano po roku
    @Query("SELECT b FROM InventoryBatch b " +
           "WHERE b.clinicId = :clinicId " +
           "AND b.inventoryItemId = :itemId " +
           "AND b.deleted = false " +
           "ORDER BY CASE WHEN b.expiryDate IS NULL THEN 1 ELSE 0 END, b.expiryDate ASC")
    List<InventoryBatch> findByItem(@Param("clinicId") UUID clinicId,
                                    @Param("itemId") UUID itemId);

    // FIFO: aktivni lotovi (qty > 0) sortirani po expiry ASC, nullovi na kraj
    @Query("SELECT b FROM InventoryBatch b " +
           "WHERE b.clinicId = :clinicId " +
           "AND b.inventoryItemId = :itemId " +
           "AND b.deleted = false " +
           "AND b.quantityOnHand > 0 " +
           "ORDER BY CASE WHEN b.expiryDate IS NULL THEN 1 ELSE 0 END, b.expiryDate ASC")
    List<InventoryBatch> findActiveByItemFifo(@Param("clinicId") UUID clinicId,
                                              @Param("itemId") UUID itemId);

    // Lotovi koji ističu pre datog datuma (za upozorenja na Dashboard-u)
    @Query("SELECT b FROM InventoryBatch b " +
           "WHERE b.clinicId = :clinicId " +
           "AND b.deleted = false " +
           "AND b.quantityOnHand > 0 " +
           "AND b.expiryDate IS NOT NULL " +
           "AND b.expiryDate <= :threshold " +
           "ORDER BY b.expiryDate ASC")
    List<InventoryBatch> findExpiringBefore(@Param("clinicId") UUID clinicId,
                                            @Param("threshold") LocalDate threshold);

    @Query("SELECT COUNT(b) FROM InventoryBatch b " +
           "WHERE b.clinicId = :clinicId " +
           "AND b.deleted = false " +
           "AND b.quantityOnHand > 0 " +
           "AND b.expiryDate IS NOT NULL " +
           "AND b.expiryDate <= :threshold")
    long countExpiringBefore(@Param("clinicId") UUID clinicId,
                             @Param("threshold") LocalDate threshold);

    // Suma količina po artiklu — za sinhronizaciju InventoryItem.quantityOnHand
    @Query("SELECT COALESCE(SUM(b.quantityOnHand), 0) FROM InventoryBatch b " +
           "WHERE b.clinicId = :clinicId " +
           "AND b.inventoryItemId = :itemId " +
           "AND b.deleted = false")
    BigDecimal sumQuantityByItem(@Param("clinicId") UUID clinicId,
                                 @Param("itemId") UUID itemId);

    // Duplikat provera (jedinstveno po (clinic, item, batchNumber))
    Optional<InventoryBatch> findByClinicIdAndInventoryItemIdAndBatchNumberAndDeletedFalse(
            UUID clinicId, UUID inventoryItemId, String batchNumber);
    
    // InventoryBatchRepository.java — dodati novu metodu:
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBatch b " +
           "WHERE b.clinicId = :clinicId " +
           "AND b.inventoryItemId = :itemId " +
           "AND b.deleted = false " +
           "AND b.quantityOnHand > 0 " +
           "ORDER BY CASE WHEN b.expiryDate IS NULL THEN 1 ELSE 0 END, b.expiryDate ASC")
    List<InventoryBatch> findActiveByItemFifoForUpdate(@Param("clinicId") UUID clinicId,
                                                        @Param("itemId") UUID itemId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM InventoryBatch b WHERE b.id = :id AND b.clinicId = :clinicId AND b.deleted = false")
    Optional<InventoryBatch> findByIdAndClinicIdAndDeletedFalseForUpdate(
            @Param("id") UUID id,
            @Param("clinicId") UUID clinicId);
    
    @Query("SELECT b.id FROM InventoryBatch b " +
    	       "WHERE b.id IN :ids AND b.clinicId = :clinicId AND b.deleted = false")
    	java.util.Set<UUID> findActiveIdsByIdInAndClinicId(@Param("ids") java.util.Collection<UUID> ids,
    	                                                    @Param("clinicId") UUID clinicId);
    
    @Query("SELECT b.id, b.batchNumber FROM InventoryBatch b WHERE b.id IN :ids AND b.clinicId = :clinicId")
    List<Object[]> findBatchNumbersByIdInAndClinicId(@Param("ids") Collection<UUID> ids,
                                                      @Param("clinicId") UUID clinicId);
}
