package com.softart.vetclinic.repository.specifications;

import com.softart.vetclinic.entity.Diagnosis;
import com.softart.vetclinic.entity.MedicalRecord;
import com.softart.vetclinic.entity.MedicalRecordDiagnosis;
import com.softart.vetclinic.entity.Pet;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class MedicalRecordSpecifications {

    private MedicalRecordSpecifications() {}

    public static Specification<MedicalRecord> inClinic(UUID clinicId) {
        return (root, query, cb) -> cb.equal(root.get("clinicId"), clinicId);
    }

    public static Specification<MedicalRecord> notDeleted() {
        return (root, query, cb) -> cb.isFalse(root.get("deleted"));
    }

    public static Specification<MedicalRecord> vetId(UUID vetId) {
        if (vetId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("vetId"), vetId);
    }

    public static Specification<MedicalRecord> dateBetween(OffsetDateTime from, OffsetDateTime to) {
        if (from == null || to == null) return null;
        return (root, query, cb) -> cb.between(root.get("createdAt"), from, to);
    }

    public static Specification<MedicalRecord> ownerId(UUID ownerId) {
        if (ownerId == null) return null;
        return (root, query, cb) -> {
            Subquery<UUID> sub = query.subquery(UUID.class);
            Root<Pet> pet = sub.from(Pet.class);
            sub.select(pet.get("id"))
               .where(cb.equal(pet.get("ownerId"), ownerId));
            return root.get("petId").in(sub);
        };
    }

    public static Specification<MedicalRecord> textSearch(String search) {
        if (search == null || search.isBlank()) return null;
        String like = "%" + search.toLowerCase() + "%";
        return (root, query, cb) -> {
            var petJoin = root.join("pet", JoinType.LEFT);
            var vetJoin = root.join("vet", JoinType.LEFT);

            Subquery<UUID> diagSub = query.subquery(UUID.class);
            Root<MedicalRecordDiagnosis> mrd = diagSub.from(MedicalRecordDiagnosis.class);
            Root<Diagnosis> d = diagSub.from(Diagnosis.class);
            diagSub.select(mrd.get("medicalRecordId"))
                   .where(cb.and(
                       cb.equal(mrd.get("diagnosisId"), d.get("id")),
                       cb.like(cb.lower(d.get("name")), like)
                   ));

            Predicate diagMatch = root.get("id").in(diagSub);

            return cb.or(
                cb.like(cb.lower(petJoin.get("name")), like),
                cb.like(cb.lower(root.get("symptoms")), like),
                cb.like(cb.lower(vetJoin.get("firstName")), like),
                cb.like(cb.lower(vetJoin.get("lastName")), like),
                cb.like(cb.lower(cb.concat(cb.concat(vetJoin.get("firstName"), " "), vetJoin.get("lastName"))), like),
                cb.like(cb.lower(root.get("recordCode")), like),
                diagMatch
            );
        };
    }

    public static Specification<MedicalRecord> withEagerLoading() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("pet", JoinType.LEFT);
                root.fetch("vet", JoinType.LEFT);
            }
            return null;
        };
    }
}