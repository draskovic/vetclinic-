package com.softart.vetclinic.dto;

import java.util.List;

public record InvoiceWithItemsResponse(
        InvoiceResponse invoice,
        List<InvoiceItemResponse> items
) {}