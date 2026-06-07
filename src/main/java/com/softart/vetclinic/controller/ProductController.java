package com.softart.vetclinic.controller;

import com.softart.vetclinic.dto.CreateProductRequest;
import com.softart.vetclinic.dto.ProductResponse;
import com.softart.vetclinic.dto.UpdateProductRequest;
import com.softart.vetclinic.enums.InventoryCategory;
import com.softart.vetclinic.mapper.ProductMapper;
import com.softart.vetclinic.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;
    private final ProductMapper productMapper;

    @GetMapping
    public Page<ProductResponse> getAll(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) InventoryCategory category,
            Pageable pageable) {
        if (pageable.getSort().isUnsorted()) {
            pageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                    Sort.by(Sort.Direction.ASC, "name"));
        }
        return productService.searchAll(clinicId, search, category, pageable)
                .map(productMapper::toResponse);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        return productMapper.toResponse(productService.findById(id, clinicId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @Valid @RequestBody CreateProductRequest request) {
        var entity = productMapper.toEntity(request);
        return productMapper.toResponse(productService.create(entity, clinicId));
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return productMapper.toResponse(
                productService.update(id, clinicId, existing -> productMapper.updateEntity(request, existing)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader("X-Clinic-Id") UUID clinicId,
            @PathVariable UUID id) {
        productService.softDelete(id, clinicId);
    }
}