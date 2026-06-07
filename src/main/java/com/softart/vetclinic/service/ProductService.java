package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Product;
import com.softart.vetclinic.enums.InventoryCategory;
import com.softart.vetclinic.exception.BadRequestException;
import com.softart.vetclinic.repository.InventoryBatchRepository;
import com.softart.vetclinic.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class ProductService extends AbstractCrudService<Product, ProductRepository> {

    private final ProductRepository productRepository;
    private final InventoryBatchRepository inventoryBatchRepository;

    public ProductService(ProductRepository productRepository,
                          InventoryBatchRepository inventoryBatchRepository) {
        super(productRepository);
        this.productRepository = productRepository;
        this.inventoryBatchRepository = inventoryBatchRepository;
    }

    @Override
    protected String getEntityName() {
        return "Product";
    }

    @Override
    protected Optional<Product> findByIdAndClinicId(UUID id, UUID clinicId) {
        return productRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<Product> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return productRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return productRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    /**
     * R2 guard: track_batches se ne sme menjati dok postoji ijedan realni (ne-DEFAULT) lot
     * za bilo koji inventory_item ovog product-a. Bez realnih lotova → promena je dozvoljena.
     */
    @Override
    @Transactional
    public Product update(UUID id, UUID clinicId, Consumer<Product> updater) {
        Product existing = findById(id, clinicId);
        Boolean originalTrackBatches = existing.getTrackBatches();

        Consumer<Product> guardedUpdater = p -> {
            updater.accept(p);
            if (!originalTrackBatches.equals(p.getTrackBatches())
                    && inventoryBatchRepository.existsRealBatchByProduct(clinicId, id)) {
                throw new BadRequestException(
                        "Praćenje po lotovima se ne može menjati dok postoje lotovi. "
                        + "Uklonite lotove ili napravite novi proizvod.");
            }
        };
        return super.update(id, clinicId, guardedUpdater);
    }

    @Transactional(readOnly = true)
    public Page<Product> searchAll(UUID clinicId, String search, InventoryCategory category, Pageable pageable) {
        if ((search == null || search.isBlank()) && category == null) {
            return findAllByClinicId(clinicId, pageable);
        }
        return productRepository.searchByClinicIdAndCategory(clinicId,
                (search == null || search.isBlank()) ? "" : search,
                category,
                pageable);
    }
}