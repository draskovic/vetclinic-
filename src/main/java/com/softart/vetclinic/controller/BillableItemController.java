package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.BillableItemResponse;
import com.softart.vetclinic.service.BillableItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/billable-items")
@RequiredArgsConstructor
public class BillableItemController {

    private final BillableItemService billableItemService;

    @GetMapping("/search")
    public List<BillableItemResponse> search(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(required = false, defaultValue = "") String q,
            @RequestParam(required = false, defaultValue = "25") int limit) {
        return billableItemService.search(clinicId, q, limit);
    }
}