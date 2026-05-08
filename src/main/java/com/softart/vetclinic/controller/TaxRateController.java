package com.softart.vetclinic.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.softart.vetclinic.dto.TaxRateResponse;
import com.softart.vetclinic.mapper.TaxRateMapper;
import com.softart.vetclinic.service.TaxRateService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/tax-rates")
@RequiredArgsConstructor
public class TaxRateController {

    private final TaxRateService taxRateService;
    private final TaxRateMapper taxRateMapper;

    /**
     * Vraća aktivne poreske stope za zadatu zemlju.
     * Default: 'RS'. Frontend ovo koristi za Select pri kreiranju Service/InventoryItem-a.
     */
    @GetMapping
    public List<TaxRateResponse> getAll(
            @RequestParam(name = "country", defaultValue = "RS") String countryCode) {
        return taxRateService.findActiveByCountryCode(countryCode).stream()
                .map(taxRateMapper::toResponse)
                .toList();
    }

    @GetMapping("/{id}")
    public TaxRateResponse getById(@PathVariable UUID id) {
        return taxRateMapper.toResponse(taxRateService.findById(id));
    }
}