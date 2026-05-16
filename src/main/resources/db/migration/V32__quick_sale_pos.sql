-- V32__quick_sale_pos.sql
-- Priprema baze za Quick Sale (POS) — direktna prodaja artikla bez vezivanja
-- za medical_record. Uvodi 6 izmena:
-- 1. inventory_item.tax_rate_id (NOT NULL, backfill po clinic.vat_payer)
-- 2. invoice_item.inventory_item_id (NULL FK — prodaja artikla)
-- 3. invoice_item.treatment_id (NULL FK — eksplicitan link treatment↔stavka)
-- 4. invoice.walk_in_customer_name (anonimni kupac)
-- 5. invoice.owner_id DROP NOT NULL (anonimne fakture)
-- 6. CHECK invoice_item: (service_id IS NOT NULL OR inventory_item_id IS NOT NULL)

-- =====================================================================
-- 1) inventory_item.tax_rate_id  (FK NOT NULL, backfill po clinic.vat_payer)
-- =====================================================================
ALTER TABLE inventory_item ADD COLUMN tax_rate_id UUID;
ALTER TABLE inventory_item ADD CONSTRAINT fk_inventory_item_tax_rate
    FOREIGN KEY (tax_rate_id) REFERENCES tax_rate(id);

-- Backfill: vat_payer=true → 'Ђ' (20%), inače → 'А' (0%, neobveznik)
UPDATE inventory_item ii
SET tax_rate_id = tr.id
FROM clinic c, tax_rate tr
WHERE c.id = ii.clinic_id
  AND tr.country_code = 'RS'
  AND tr.label = CASE
        WHEN COALESCE(c.vat_payer, false) = true THEN 'Ђ'
        ELSE 'А'
      END;

ALTER TABLE inventory_item ALTER COLUMN tax_rate_id SET NOT NULL;

-- =====================================================================
-- 2) invoice_item.inventory_item_id  (FK NULL — direktna prodaja artikla)
-- =====================================================================
ALTER TABLE invoice_item ADD COLUMN inventory_item_id UUID;
ALTER TABLE invoice_item ADD CONSTRAINT fk_invoice_item_inventory_item
    FOREIGN KEY (inventory_item_id) REFERENCES inventory_item(id);

CREATE INDEX idx_invoice_item_inventory_item
    ON invoice_item(inventory_item_id)
    WHERE inventory_item_id IS NOT NULL AND deleted = false;

-- =====================================================================
-- 3) invoice_item.treatment_id  (FK NULL — eksplicitan link treatment↔stavka)
-- =====================================================================
ALTER TABLE invoice_item ADD COLUMN treatment_id UUID;
ALTER TABLE invoice_item ADD CONSTRAINT fk_invoice_item_treatment
    FOREIGN KEY (treatment_id) REFERENCES treatment(id);

CREATE INDEX idx_invoice_item_treatment
    ON invoice_item(treatment_id)
    WHERE treatment_id IS NOT NULL AND deleted = false;

-- =====================================================================
-- 4) invoice.walk_in_customer_name  (anonimni kupac)
-- =====================================================================
ALTER TABLE invoice ADD COLUMN walk_in_customer_name VARCHAR(200);

-- =====================================================================
-- 5) invoice.owner_id  DROP NOT NULL (anonimne fakture)
-- =====================================================================
ALTER TABLE invoice ALTER COLUMN owner_id DROP NOT NULL;

-- =====================================================================
-- 6) CHECK invoice_item: stavka mora imati service_id ILI inventory_item_id
-- =====================================================================
-- VAŽNO: ako u tvojoj bazi postoje invoice_item zapisi sa service_id=NULL
-- (stari slobodan tekst), ovaj constraint će puknuti. Provera pre primene:
--   SELECT COUNT(*) FROM invoice_item WHERE service_id IS NULL AND deleted=false;
-- Ako je >0: backfill ručno (npr. SET inventory_item_id) ili obriši orphan zapise.
ALTER TABLE invoice_item ADD CONSTRAINT chk_invoice_item_source
    CHECK (service_id IS NOT NULL OR inventory_item_id IS NOT NULL);