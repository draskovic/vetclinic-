package com.softart.vetclinic.service;

import com.softart.vetclinic.dto.QuickSaleLineRequest;
import com.softart.vetclinic.dto.QuickSaleRequest;
import com.softart.vetclinic.entity.Invoice;
import com.softart.vetclinic.entity.InvoiceItem;
import com.softart.vetclinic.entity.InventoryItem;
import com.softart.vetclinic.entity.Payment;
import com.softart.vetclinic.enums.InvoiceStatus;
import com.softart.vetclinic.exception.BadRequestException;
import com.softart.vetclinic.exception.ResourceNotFoundException;
import com.softart.vetclinic.repository.InventoryItemRepository;
import com.softart.vetclinic.repository.InvoiceItemRepository;
import com.softart.vetclinic.repository.OwnerRepository;
import com.softart.vetclinic.repository.PaymentRepository;
import com.softart.vetclinic.repository.ServiceRepository;
import com.softart.vetclinic.util.InvoiceItemTotals;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Quick Sale (POS) — atomski kreira Invoice + InvoiceItem[] + Payment + auto-OUT
 * inventarne transakcije u JEDNOJ @Transactional metodi.
 *
 * Totals (subtotal/tax/discount/total) se računaju SERVER-SIDE iz lines pre INSERT-a
 * invoice-a → nema kasnijeg UPDATE-a invoice tabele → RLS pravilo iz CLAUDE.md je
 * poštovano (UPDATE invoice iz servisa unutar iste transakcije ne radi pouzdano).
 *
 * Payment Smart A (tendered/change model):
 *   payment.amount = min(tenderedAmount, total)
 *   changeAmount   = max(tenderedAmount - total, 0)   — samo u response, ne perzistira
 *   status         = tenderedAmount >= total ? PAID : PARTIALLY_PAID  (sa epsilon-om 0.01)
 *   payment.note   = default "Primljeno: X, vraćen kusur: Y" ako change > 0 (override kroz request.paymentNote)
 */
@Service
public class QuickSaleService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final BigDecimal PAYMENT_EPSILON = new BigDecimal("0.01");

    private final InvoiceService invoiceService;
    private final InvoiceItemRepository invoiceItemRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceRepository serviceRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final OwnerRepository ownerRepository;
    private final TaxRateSnapshotApplier taxRateSnapshotApplier;
    private final InventoryDeductionService inventoryDeductionService;

    public QuickSaleService(InvoiceService invoiceService,
                            InvoiceItemRepository invoiceItemRepository,
                            PaymentRepository paymentRepository,
                            ServiceRepository serviceRepository,
                            InventoryItemRepository inventoryItemRepository,
                            OwnerRepository ownerRepository,
                            TaxRateSnapshotApplier taxRateSnapshotApplier,
                            InventoryDeductionService inventoryDeductionService) {
        this.invoiceService = invoiceService;
        this.invoiceItemRepository = invoiceItemRepository;
        this.paymentRepository = paymentRepository;
        this.serviceRepository = serviceRepository;
        this.inventoryItemRepository = inventoryItemRepository;
        this.ownerRepository = ownerRepository;
        this.taxRateSnapshotApplier = taxRateSnapshotApplier;
        this.inventoryDeductionService = inventoryDeductionService;
    }

    /**
     * Composite rezultat — kontroler mapira u QuickSaleResponse.
     */
    public record QuickSaleResult(
            Invoice invoice,
            List<InvoiceItem> items,
            Payment payment,
            BigDecimal changeAmount
    ) {}

    @Transactional
    public QuickSaleResult createSale(UUID clinicId, QuickSaleRequest request) {

        // ============================================================
        // 1) Validacije ownership-a (ako je ownerId prosleđen)
        // ============================================================
        if (request.ownerId() != null
                && !ownerRepository.existsByIdAndClinicIdAndDeletedFalse(request.ownerId(), clinicId)) {
            throw new ResourceNotFoundException("Owner", "id", request.ownerId());
        }

        // ============================================================
        // 2) Validacija svake linije: mora imati serviceId ILI inventoryItemId
        // ============================================================
        for (QuickSaleLineRequest line : request.lines()) {
            if (line.serviceId() == null && line.inventoryItemId() == null) {
                throw new BadRequestException(
                        "Svaka stavka mora imati serviceId ili inventoryItemId.");
            }
        }

        // ============================================================
        // 3) Batch fetch service-a i inventory item-a (po 1 SQL upit)
        // ============================================================
        List<UUID> serviceIds = request.lines().stream()
                .map(QuickSaleLineRequest::serviceId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        List<UUID> inventoryItemIds = request.lines().stream()
                .map(QuickSaleLineRequest::inventoryItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, com.softart.vetclinic.entity.Service> serviceMap = serviceIds.isEmpty()
                ? Map.of()
                : serviceRepository.findAllById(serviceIds).stream()
                        .filter(s -> !Boolean.TRUE.equals(s.getDeleted())
                                && clinicId.equals(s.getClinicId()))
                        .collect(Collectors.toMap(
                                com.softart.vetclinic.entity.Service::getId, s -> s));

        Map<UUID, InventoryItem> inventoryItemMap = inventoryItemIds.isEmpty()
                ? Map.of()
                : inventoryItemRepository.findAllById(inventoryItemIds).stream()
                        .filter(i -> !Boolean.TRUE.equals(i.getDeleted())
                                && clinicId.equals(i.getClinicId()))
                        .collect(Collectors.toMap(InventoryItem::getId, i -> i));

        // ============================================================
        // 4) Build InvoiceItem entitete + akumuliraj totals
        //    (ista formula kao InvoiceTotalsRecalculationService.recalculate)
        // ============================================================
        List<InvoiceItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal taxAmount = BigDecimal.ZERO;
        BigDecimal discountAmount = BigDecimal.ZERO;

        int sortOrder = 1;
        for (QuickSaleLineRequest lineReq : request.lines()) {
            com.softart.vetclinic.entity.Service service = lineReq.serviceId() != null
                    ? serviceMap.get(lineReq.serviceId()) : null;
            InventoryItem inventoryItem = lineReq.inventoryItemId() != null
                    ? inventoryItemMap.get(lineReq.inventoryItemId()) : null;

            if (lineReq.serviceId() != null && service == null) {
                throw new ResourceNotFoundException("Service", "id", lineReq.serviceId());
            }
            if (lineReq.inventoryItemId() != null && inventoryItem == null) {
                throw new ResourceNotFoundException("InventoryItem", "id", lineReq.inventoryItemId());
            }

            // Resolve description (klijent override → service.name → inventoryItem.name)
            String description = lineReq.description();
            if (description == null || description.isBlank()) {
                description = service != null ? service.getName() : inventoryItem.getName();
            }

            // Resolve unitPrice (klijent override → service.price → inventoryItem.sellPrice)
            BigDecimal unitPrice = lineReq.unitPrice();
            if (unitPrice == null) {
                unitPrice = service != null ? service.getPrice() : inventoryItem.getSellPrice();
            }
            if (unitPrice == null) {
                throw new BadRequestException(
                        "Cena nije definisana za stavku '" + description + "'.");
            }

            // Resolve taxRateId (klijent override → service.taxRateId → inventoryItem.taxRateId)
            UUID resolvedTaxRateId = lineReq.taxRateId();
            if (resolvedTaxRateId == null && service != null) {
                resolvedTaxRateId = service.getTaxRateId();
            }
            if (resolvedTaxRateId == null && inventoryItem != null) {
                resolvedTaxRateId = inventoryItem.getTaxRateId();
            }

            BigDecimal discountPercent = lineReq.discountPercent() != null
                    ? lineReq.discountPercent() : BigDecimal.ZERO;

            InvoiceItem item = new InvoiceItem();
            item.setClinicId(clinicId);
            // invoiceId — postavlja se posle inserta invoice-a
            item.setServiceId(lineReq.serviceId());
            item.setInventoryItemId(lineReq.inventoryItemId());
            // treatmentId — null za Quick Sale
            item.setDescription(description);
            item.setQuantity(lineReq.quantity());
            item.setUnitPrice(unitPrice);
            item.setDiscountPercent(discountPercent);
            item.setSortOrder(sortOrder++);

            // Snapshot PDV stope (ako resolvedTaxRateId == null, applier fallback-uje na clinic default)
            taxRateSnapshotApplier.apply(item, resolvedTaxRateId, clinicId);

            // Compute lineTotal — server je single source of truth (klijentova vrednost se ignoriše)
            item.setLineTotal(InvoiceItemTotals.computeLineTotal(item));

            // Akumuliraj totals (ista formula kao recalculate)
            BigDecimal base = unitPrice.multiply(lineReq.quantity());
            BigDecimal discount = base.multiply(
                    discountPercent.divide(HUNDRED, 4, RoundingMode.HALF_UP));
            BigDecimal net = base.subtract(discount);
            BigDecimal tax = net.multiply(
                    item.getTaxRatePercent().divide(HUNDRED, 4, RoundingMode.HALF_UP));

            subtotal = subtotal.add(net);
            taxAmount = taxAmount.add(tax);
            discountAmount = discountAmount.add(discount);

            items.add(item);
        }

        BigDecimal totalScaled = subtotal.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        // ============================================================
        // 5) Payment računanje (Smart A — tendered/change)
        // ============================================================
        BigDecimal tendered = request.tenderedAmount();
        if (totalScaled.signum() > 0 && tendered.signum() <= 0) {
            throw new BadRequestException("Iznos primljen od kupca mora biti veći od 0.");
        }
        BigDecimal paid = tendered.min(totalScaled);
        BigDecimal change = tendered.subtract(totalScaled).max(BigDecimal.ZERO);

        InvoiceStatus status = (tendered.compareTo(totalScaled.subtract(PAYMENT_EPSILON)) >= 0)
                ? InvoiceStatus.PAID
                : InvoiceStatus.PARTIALLY_PAID;

        // ============================================================
        // 6) Build i INSERT invoice (sa već popunjenim totals)
        // ============================================================
        String currency = (request.currency() != null && !request.currency().isBlank())
                ? request.currency() : "RSD";

        Invoice invoice = new Invoice();
        invoice.setOwnerId(request.ownerId());
        invoice.setWalkInCustomerName(request.walkInCustomerName());
        invoice.setLocationId(request.locationId());
        invoice.setIssuedAt(request.issuedAt() != null ? request.issuedAt() : OffsetDateTime.now());
        invoice.setStatus(status);
        invoice.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        invoice.setTaxAmount(taxAmount.setScale(2, RoundingMode.HALF_UP));
        invoice.setDiscountAmount(discountAmount.setScale(2, RoundingMode.HALF_UP));
        invoice.setTotal(totalScaled);
        invoice.setCurrency(currency);
        invoice.setNote(request.note());

        Invoice savedInvoice = invoiceService.create(invoice, clinicId);

        // ============================================================
        // 7) Postavi invoiceId na stavke i saveAll (atomic INSERT-i)
        // ============================================================
        for (InvoiceItem item : items) {
            item.setInvoiceId(savedInvoice.getId());
        }
        List<InvoiceItem> savedItems = invoiceItemRepository.saveAll(items);

        // ============================================================
        // 8) Auto-OUT inventarne transakcije za stavke sa inventoryItemId
        //    (REQUIRED propagation — ulazi u istu transakciju)
        // ============================================================
        for (InvoiceItem item : savedItems) {
            if (item.getInventoryItemId() != null) {
                inventoryDeductionService.deductForInvoiceItem(
                        clinicId,
                        item.getInventoryItemId(),
                        item.getQuantity(),
                        item.getId(),
                        request.vetId());
            }
        }

        // ============================================================
        // 9) INSERT payment (note default ako change > 0)
        // ============================================================
        Payment payment = new Payment();
        payment.setClinicId(clinicId);
        payment.setInvoiceId(savedInvoice.getId());
        payment.setAmount(paid);
        payment.setMethod(request.paymentMethod());
        payment.setPaidAt(request.paidAt() != null ? request.paidAt() : OffsetDateTime.now());
        payment.setReferenceNumber(request.paymentReferenceNumber());

        String noteText;
        if (request.paymentNote() != null && !request.paymentNote().isBlank()) {
            noteText = request.paymentNote();
        } else if (change.compareTo(BigDecimal.ZERO) > 0) {
            noteText = "Primljeno: " + tendered + " " + currency
                    + ", vraćen kusur: " + change + " " + currency;
        } else {
            noteText = null;
        }
        payment.setNote(noteText);

        Payment savedPayment = paymentRepository.save(payment);

        return new QuickSaleResult(savedInvoice, savedItems, savedPayment, change);
    }
}