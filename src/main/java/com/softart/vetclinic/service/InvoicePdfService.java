package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Clinic;
import com.softart.vetclinic.entity.Invoice;
import com.softart.vetclinic.entity.InvoiceItem;
import com.softart.vetclinic.entity.Owner;
import com.softart.vetclinic.enums.InvoiceStatus;
import com.softart.vetclinic.repository.ClinicRepository;
import com.softart.vetclinic.repository.InvoiceItemRepository;
import com.softart.vetclinic.repository.InvoiceRepository;
import com.softart.vetclinic.repository.OwnerRepository;
import com.softart.vetclinic.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;


@Slf4j
@Service
@RequiredArgsConstructor
public class InvoicePdfService {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceItemRepository invoiceItemRepository;
    private final OwnerRepository ownerRepository;
    private final ClinicRepository clinicRepository;
    private final TemplateEngine templateEngine;
    private final ClinicLogoHelper clinicLogoHelper;


    private static final Map<InvoiceStatus, String> STATUS_LABELS = Map.of(
            InvoiceStatus.DRAFT, "Nacrt",
            InvoiceStatus.ISSUED, "Izdata",
            InvoiceStatus.PAID, "Plaćena",
            InvoiceStatus.PARTIALLY_PAID, "Delimično plaćena",
            InvoiceStatus.OVERDUE, "Dospela",
            InvoiceStatus.CANCELLED, "Otkazana",
            InvoiceStatus.REFUNDED, "Refundirana"
    );

    @Transactional(readOnly = true)
    public byte[] generatePdf(UUID invoiceId, UUID clinicId) {
        // 1. Fetch data
    	Invoice invoice = invoiceRepository.findByIdAndClinicIdAndDeletedFalse(invoiceId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", "id", invoiceId));

        Owner owner = ownerRepository.findById(invoice.getOwnerId())
                .orElseThrow(() -> new ResourceNotFoundException("Owner", "id", invoice.getOwnerId()));

        Clinic clinic = clinicRepository.findById(clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Clinic", "id", clinicId));

        List<InvoiceItem> items = invoiceItemRepository.findByClinicIdAndInvoiceIdAndDeletedFalseOrderBySortOrderAsc(
                clinicId, invoiceId);


        // 2. Build Thymeleaf context
        Context context = new Context();
        context.setVariable("invoice", invoice);
        context.setVariable("owner", owner);
        context.setVariable("clinic", clinic);
        context.setVariable("items", items);
        context.setVariable("statusLabel", STATUS_LABELS.getOrDefault(invoice.getStatus(), invoice.getStatus().name()));
        context.setVariable("logoDataUri", clinicLogoHelper.toDataUri(clinic));


        // 3. Render HTML
        String html = templateEngine.process("invoice", context);

        // 4. Convert HTML to PDF
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
        	ITextRenderer renderer = new ITextRenderer();

            // Register DejaVu Sans font for Serbian Latin characters (ć, č, š, ž, đ)
            String fontPath = getClass().getClassLoader()
                    .getResource("fonts/DejaVuSans.ttf").toURI().toString();
            renderer.getFontResolver().addFont(fontPath, com.lowagie.text.pdf.BaseFont.IDENTITY_H, true);


            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to generate PDF for invoice {}: {}", invoiceId, e.getMessage(), e);
            throw new RuntimeException("Greška pri generisanju PDF-a", e);
        }
    }
}
