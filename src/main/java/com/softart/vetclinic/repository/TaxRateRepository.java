package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.TaxRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TaxRateRepository extends JpaRepository<TaxRate, UUID> {

    @Query("SELECT t FROM TaxRate t " +
           "WHERE t.countryCode = :countryCode " +
           "AND t.deleted = false " +
           "AND t.active = true " +
           "ORDER BY t.label ASC")
    List<TaxRate> findActiveByCountryCode(@Param("countryCode") String countryCode);

    @Query("SELECT t FROM TaxRate t " +
           "WHERE t.countryCode = :countryCode " +
           "AND t.label = :label " +
           "AND t.deleted = false")
    Optional<TaxRate> findByCountryCodeAndLabel(@Param("countryCode") String countryCode,
                                                 @Param("label") String label);

    Optional<TaxRate> findByIdAndDeletedFalse(UUID id);
}