package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.TaxRate;
import com.softart.vetclinic.exception.ResourceNotFoundException;
import com.softart.vetclinic.repository.TaxRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaxRateService {

    private final TaxRateRepository taxRateRepository;

    public List<TaxRate> findActiveByCountryCode(String countryCode) {
        return taxRateRepository.findActiveByCountryCode(countryCode);
    }

    public TaxRate findById(UUID id) {
        return taxRateRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TaxRate sa id " + id + " nije pronađen"));
    }

    public Optional<TaxRate> findByCountryCodeAndLabel(String countryCode, String label) {
        return taxRateRepository.findByCountryCodeAndLabel(countryCode, label);
    }
}