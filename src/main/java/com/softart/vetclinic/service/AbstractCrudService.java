package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.BaseEntity;
import com.softart.vetclinic.exception.ResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Genericki bazni servis za CRUD operacije nad entitetima koji extends BaseEntity.
 * Pruza standardne operacije: findById, findAll (paginirano), create, update, softDelete.
 * Svi upiti su clinic-scoped i soft-delete aware.
 *
 * @param <T> tip entiteta (extends BaseEntity)
 * @param <R> tip repozitorijuma (extends JpaRepository)
 */
public abstract class AbstractCrudService<T extends BaseEntity, R extends JpaRepository<T, UUID>> {

    protected final R repository;

    protected AbstractCrudService(R repository) {
        this.repository = repository;
    }

    /**
     * Vraca naziv entiteta za poruke o greskama.
     */
    protected abstract String getEntityName();

    /**
     * Pronalazi entitet po ID-u i clinicId-u (soft-delete aware).
     * Svaki konkretni servis mora implementirati jer koristi specifican repository metod.
     */
    protected abstract java.util.Optional<T> findByIdAndClinicId(UUID id, UUID clinicId);

    /**
     * Vraca stranicu entiteta za datu kliniku (soft-delete aware).
     * Svaki konkretni servis mora implementirati jer koristi specifican repository metod.
     */
    protected abstract Page<T> findAllByClinicId(UUID clinicId, Pageable pageable);

    /**
     * Proverava da li entitet postoji po ID-u i clinicId-u (soft-delete aware).
     */
    protected abstract boolean existsByIdAndClinicId(UUID id, UUID clinicId);

    /**
     * Validacija pre kreiranja. Override-uj za poslovnu logiku.
     * Default implementacija ne radi nista.
     */
    protected void validateForCreate(T entity) {
        // Override u konkretnom servisu
    }

    /**
     * Validacija pre azuriranja. Override-uj za poslovnu logiku.
     * Default implementacija ne radi nista.
     */
    protected void validateForUpdate(T existing, T updated) {
        // Override u konkretnom servisu
    }

    // ========================================================================
    // CRUD operacije
    // ========================================================================

    /**
     * Pronalazi entitet po ID-u u okviru klinike.
     * @throws ResourceNotFoundException ako entitet ne postoji ili je obrisan
     */
    @Transactional(readOnly = true)
    public T findById(UUID id, UUID clinicId) {
        return findByIdAndClinicId(id, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException(getEntityName(), "id", id));
    }

    /**
     * Vraca paginiranu listu entiteta za kliniku (bez obrisanih).
     */
    @Transactional(readOnly = true)
    public Page<T> findAll(UUID clinicId, Pageable pageable) {
        return findAllByClinicId(clinicId, pageable);
    }

    /**
     * Kreira novi entitet. Automatski setuje clinicId.
     */
    @Transactional
    public  T create(T entity, UUID clinicId) {
        entity.setClinicId(clinicId);
        entity.setDeleted(false);
        validateForCreate(entity);
        return repository.save(entity);
    }

    /**
     * Azurira postojeci entitet koristeci updater funkciju.
     * Updater prima postojeci entitet i modifikuje ga.
     * @throws ResourceNotFoundException ako entitet ne postoji ili je obrisan
     */
    @Transactional
    public T update(UUID id, UUID clinicId, Consumer<T> updater) {
        T existing = findById(id, clinicId);
        updater.accept(existing);
        // clinicId i id se ne mogu menjati
        existing.setClinicId(clinicId);
        validateForUpdate(existing, existing);
        return repository.save(existing);
    }

    /**
     * Soft delete - oznacava entitet kao obrisan.
     * @throws ResourceNotFoundException ako entitet ne postoji ili je vec obrisan
     */
    @Transactional
    public void softDelete(UUID id, UUID clinicId) {
        T entity = findById(id, clinicId);
        entity.setDeleted(true);
        entity.setDeletedAt(OffsetDateTime.now());
        repository.save(entity);
    }

    // ========================================================================
    // Helper metode za FK validaciju (koriste ih konkretni servisi)
    // ========================================================================

    /**
     * Proverava da FK entitet postoji i pripada istoj klinici.
     * Koristi se u validateForCreate/validateForUpdate.
     */
    protected void requireExists(boolean exists, String referencedEntity, String field, Object value) {
        if (!exists) {
            throw new ResourceNotFoundException(referencedEntity, field, value);
        }
    }
}
