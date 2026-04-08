package com.softart.vetclinic.repository;

import com.softart.vetclinic.entity.MedicalRecordDiagnosis;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;



@Repository
public interface MedicalRecordDiagnosisRepository extends JpaRepository<MedicalRecordDiagnosis, UUID> {

    List<MedicalRecordDiagnosis> findByClinicIdAndMedicalRecordId(UUID clinicId, UUID medicalRecordId);

    List<MedicalRecordDiagnosis> findByClinicIdAndMedicalRecordIdIn(UUID clinicId, List<UUID> medicalRecordIds);

    @Transactional
    @Modifying
    @Query("DELETE FROM MedicalRecordDiagnosis mrd WHERE mrd.clinicId = :clinicId AND mrd.medicalRecordId = :medicalRecordId")
    void deleteByClinicIdAndMedicalRecordId(@Param("clinicId") UUID clinicId, @Param("medicalRecordId") UUID medicalRecordId);

}
