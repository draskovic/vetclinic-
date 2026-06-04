package com.softart.vetclinic.service;

import com.softart.vetclinic.dto.BillableItemResponse;
import com.softart.vetclinic.entity.InventoryItem;
import com.softart.vetclinic.entity.TaxRate;
import com.softart.vetclinic.enums.BillableItemType;
import com.softart.vetclinic.repository.TaxRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BillableItemService {

    private final ServiceService serviceService;
    private final InventoryItemService inventoryItemService;
    private final TaxRateRepository taxRateRepository;

    @Transactional(readOnly = true)
    public List<BillableItemResponse> search(UUID clinicId, String search, int limit) {
        String q = (search == null) ? "" : search;
        int safeLimit = Math.min(Math.max(limit, 1), 50);
        Pageable page = PageRequest.of(0, safeLimit, Sort.by("name"));

        List<com.softart.vetclinic.entity.Service> services =
                serviceService.searchAll(clinicId, q, null, page).getContent().stream()
                        .filter(s -> Boolean.TRUE.equals(s.getActive()))
                        .toList();

        List<InventoryItem> items =
                inventoryItemService.searchAll(clinicId, q, null, page).getContent().stream()
                        .filter(i -> Boolean.TRUE.equals(i.getActive()))
                        .toList();

        // Batch PDV (1 upit za sve taxRateId-jeve; tax_rate je globalan šifarnik, bez RLS)
        Set<UUID> taxRateIds = new HashSet<>();
        services.forEach(s -> taxRateIds.add(s.getTaxRateId()));
        items.forEach(i -> taxRateIds.add(i.getTaxRateId()));
        Map<UUID, TaxRate> taxRates = taxRateIds.isEmpty()
                ? Map.of()
                : taxRateRepository.findAllById(taxRateIds).stream()
                        .collect(Collectors.toMap(TaxRate::getId, tr -> tr));

        List<BillableItemResponse> result = new ArrayList<>(services.size() + items.size());
        for (var s : services) {
            TaxRate tr = taxRates.get(s.getTaxRateId());
            result.add(new BillableItemResponse(
                    BillableItemType.SERVICE, s.getId(), s.getName(), s.getSku(), s.getUnit(),
                    s.getPrice(), s.getTaxRateId(),
                    tr != null ? tr.getLabel() : null,
                    tr != null ? tr.getPercent() : null,
                    null, null));
        }
        for (var i : items) {
            TaxRate tr = taxRates.get(i.getTaxRateId());
            result.add(new BillableItemResponse(
                    BillableItemType.ITEM, i.getId(), i.getName(), i.getSku(), i.getUnit(),
                    i.getSellPrice(), i.getTaxRateId(),
                    tr != null ? tr.getLabel() : null,
                    tr != null ? tr.getPercent() : null,
                    i.getQuantityOnHand(), i.getTrackBatches()));
        }
        return result;
    }
}