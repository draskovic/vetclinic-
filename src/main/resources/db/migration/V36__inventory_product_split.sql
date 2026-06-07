-- ============================================================
-- V36: SPLIT lagera — product (matični) / inventory_item (per-lokacija) / inventory_batch (jedini izvor stanja)
-- ============================================================
-- Baza prazna (lager se ne koristi na produkciji) → data migracija trivijalna, ali korektna.
-- Flyway izvršava ceo fajl u JEDNOJ tx-i (atomski).
-- PAŽNJA: NE restartuj backend dok entiteti (sledeći koraci) nisu refaktorisani — fajl DROP-uje
-- kolone koje stari backend još čita.

-- ------------------------------------------------------------
-- 1) product (matični šifarnik)
-- ------------------------------------------------------------
CREATE TABLE product (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    name            VARCHAR(200)    NOT NULL,
    sku             VARCHAR(50),
    category        VARCHAR(50)     NOT NULL
                        CHECK (category IN ('MEDICATION', 'SUPPLY', 'EQUIPMENT')),
    unit            VARCHAR(20),
    track_batches   BOOLEAN         NOT NULL DEFAULT false,
    active          BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_product_clinic_category ON product(clinic_id, category) WHERE deleted = false;
CREATE INDEX idx_product_sku ON product(clinic_id, sku) WHERE deleted = false;

ALTER TABLE product ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON product
    USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'vetapp_user') THEN
        EXECUTE 'GRANT ALL ON product TO vetapp_user';
    END IF;
END $$;

CREATE TRIGGER trg_product_updated_at
    BEFORE UPDATE ON product
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ------------------------------------------------------------
-- 2) inventory_item.product_id + migracija 1:1 (svaki item → jedan product)
-- ------------------------------------------------------------
ALTER TABLE inventory_item ADD COLUMN product_id UUID;

-- temp kolona za 1:1 mapiranje generisanih product.id ↔ izvorni inventory_item.id
ALTER TABLE product ADD COLUMN _migrate_src_item_id UUID;
INSERT INTO product (clinic_id, name, sku, category, unit, track_batches, active, deleted, deleted_at, _migrate_src_item_id)
SELECT clinic_id, name, sku, category, unit, track_batches, active, deleted, deleted_at, id
FROM inventory_item;
UPDATE inventory_item ii
SET product_id = p.id
FROM product p
WHERE p._migrate_src_item_id = ii.id;
ALTER TABLE product DROP COLUMN _migrate_src_item_id;

-- ------------------------------------------------------------
-- 3) inventory_batch: is_default + indeksi (jedan DEFAULT po artiklu + index-only SUM)
-- ------------------------------------------------------------
ALTER TABLE inventory_batch ADD COLUMN is_default BOOLEAN NOT NULL DEFAULT false;

CREATE UNIQUE INDEX idx_batch_one_default
    ON inventory_batch(inventory_item_id)
    WHERE is_default = true AND deleted = false;
CREATE INDEX idx_batch_item_qty
    ON inventory_batch(inventory_item_id, quantity_on_hand)
    WHERE deleted = false;

-- ------------------------------------------------------------
-- 4) DEFAULT batch po artiklu (preuzima stanje sa inventory_item)
--    ne-batch: qty = item.quantity_on_hand; batch: qty = item.qty − SUM(postojeće serije)
-- ------------------------------------------------------------
INSERT INTO inventory_batch
    (clinic_id, inventory_item_id, batch_number, quantity_on_hand, received_at, cost_price, is_default)
SELECT
    ii.clinic_id, ii.id, 'DEFAULT',
    ii.quantity_on_hand - COALESCE(
        (SELECT SUM(b.quantity_on_hand) FROM inventory_batch b
         WHERE b.inventory_item_id = ii.id AND b.deleted = false), 0),
    CURRENT_DATE, ii.cost_price, true
FROM inventory_item ii
WHERE ii.deleted = false;

-- ------------------------------------------------------------
-- 5) service_inventory_item: inventory_item_id → product_id (BOM na nivou klinike)
-- ------------------------------------------------------------
ALTER TABLE service_inventory_item ADD COLUMN product_id UUID;
UPDATE service_inventory_item sii
SET product_id = ii.product_id
FROM inventory_item ii
WHERE ii.id = sii.inventory_item_id;

DROP INDEX IF EXISTS idx_sii_unique;
DROP INDEX IF EXISTS idx_sii_item;
ALTER TABLE service_inventory_item DROP COLUMN inventory_item_id;
ALTER TABLE service_inventory_item
    ADD CONSTRAINT fk_sii_product FOREIGN KEY (product_id) REFERENCES product(id);
ALTER TABLE service_inventory_item ALTER COLUMN product_id SET NOT NULL;

CREATE UNIQUE INDEX idx_sii_unique
    ON service_inventory_item(clinic_id, service_id, product_id)
    WHERE deleted = false;
CREATE INDEX idx_sii_product ON service_inventory_item(clinic_id, product_id);

-- ------------------------------------------------------------
-- 6) inventory_item: finalizuj product_id FK, izbaci preseljene kolone
-- ------------------------------------------------------------
ALTER TABLE inventory_item
    ADD CONSTRAINT fk_inventory_item_product FOREIGN KEY (product_id) REFERENCES product(id);
ALTER TABLE inventory_item ALTER COLUMN product_id SET NOT NULL;

DROP INDEX IF EXISTS idx_inventory_clinic_category;
ALTER TABLE inventory_item
    DROP COLUMN name,
    DROP COLUMN sku,
    DROP COLUMN category,
    DROP COLUMN unit,
    DROP COLUMN track_batches,
    DROP COLUMN quantity_on_hand,
    DROP COLUMN cost_price,
    DROP COLUMN expiry_date;

CREATE INDEX idx_inventory_item_product ON inventory_item(clinic_id, product_id) WHERE deleted = false;