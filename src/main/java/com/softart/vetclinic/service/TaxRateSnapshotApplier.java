package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Clinic;
import com.softart.vetclinic.entity.InvoiceItem;
import com.softart.vetclinic.entity.Service;
import com.softart.vetclinic.entity.TaxRate;
import com.softart.vetclinic.repository.ClinicRepository;
import com.softart.vetclinic.repository.ServiceRepository;
import com.softart.vetclinic.repository.TaxRateRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Centralni helper za snapshot PDV stope na invoice_item.
 * Resolution order:
 *  1. requestedTaxRateId — eksplicitno prosleđen
 *  2. service.taxRateId — ako je serviceId prosleđen
 *  3. default po klinici (Ђ za PDV obveznika, А za neobveznika)
 *
 * Snapshot polja (taxRateId, taxRateLabel, taxRatePercent) se popunjavaju iz tax_rate šifarnika
 * tako da kasnija promena šifarnika NE utiče na već fakturisane stavke.
 */
@Component
public class TaxRateSnapshotApplier {

    private final TaxRateRepository taxRateRepository;
    private final ServiceRepository serviceRepository;
    private final ClinicRepository clinicRepository;

    public TaxRateSnapshotApplier(TaxRateRepository taxRateRepository,
                                  ServiceRepository serviceRepository,
                                  ClinicRepository clinicRepository) {
        this.taxRateRepository = taxRateRepository;
        this.serviceRepository = serviceRepository;
        this.clinicRepository = clinicRepository;
    }

    /**
     * Pun signature — koristi se kad imamo requestedTaxRateId iz request-a (npr. InvoiceItemController.create/update).
     */
    public void apply(InvoiceItem entity, UUID requestedTaxRateId, UUID serviceId, UUID clinicId) {
        UUID taxRateId = requestedTaxRateId;

        if (taxRateId == null && serviceId != null) {
            taxRateId = serviceRepository.findByIdAndClinicIdAndDeletedFalse(serviceId, clinicId)
                    .map(Service::getTaxRateId)
                    .orElse(null);
        }
        if (taxRateId == null) {
            taxRateId = resolveDefaultTaxRateId(clinicId);
        }

        UUID finalTaxRateId = taxRateId;
        TaxRate tr = taxRateRepository.findByIdAndDeletedFalse(finalTaxRateId)
                .orElseThrow(() -> new IllegalStateException(
                        "TaxRate sa id " + finalTaxRateId + " nije pronađen"));

        entity.setTaxRateId(tr.getId());
        entity.setTaxRateLabel(tr.getLabel());
        entity.setTaxRatePercent(tr.getPercent());
    }

    /**
     * Skraćena varijanta — koristi se kad već imamo razreseni taxRateId iz service-a
     * (npr. TreatmentController, TreatmentProtocolController).
     */
    public void apply(InvoiceItem entity, UUID serviceTaxRateId, UUID clinicId) {
        apply(entity, serviceTaxRateId, null, clinicId);
    }

    private UUID resolveDefaultTaxRateId(UUID clinicId) {
        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new IllegalStateException("Klinika ne postoji: " + clinicId));
        String label = Boolean.TRUE.equals(clinic.getVatPayer()) ? "Ђ" : "А";
        return taxRateRepository.findByCountryCodeAndLabel("RS", label)
                .orElseThrow(() -> new IllegalStateException(
                        "Default TaxRate '" + label + "' (RS) nije pronađen u šifarniku"))
                .getId();
    }
}