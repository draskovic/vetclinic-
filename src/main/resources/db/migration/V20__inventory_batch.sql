-- ============================================================
-- V20: inventory_batch — praćenje artikala po seriji/lotu (FIFO)
-- ============================================================

-- 1) Flag na inventory_item: da li se artikal prati po lotovima
ALTER TABLE inventory_item
    ADD COLUMN track_batches BOOLEAN NOT NULL DEFAULT false;

-- 2) Tabela inventory_batch
CREATE TABLE inventory_batch (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id         UUID            NOT NULL REFERENCES clinic(id),
    inventory_item_id UUID            NOT NULL REFERENCES inventory_item(id),
    batch_number      VARCHAR(100)    NOT NULL,
    expiry_date       DATE,
    quantity_on_hand  DECIMAL(10,2)   NOT NULL DEFAULT 0,
    received_at       DATE            NOT NULL,
    supplier          VARCHAR(255),
    cost_price        DECIMAL(12,2),
    notes             TEXT,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted           BOOLEAN         NOT NULL DEFAULT false,
    deleted_at        TIMESTAMPTZ,
    version           INTEGER         NOT NULL DEFAULT 0
);

-- Indeksi
CREATE UNIQUE INDEX idx_batch_unique
    ON inventory_batch(clinic_id, inventory_item_id, batch_number)
    WHERE deleted = false;
CREATE INDEX idx_batch_clinic ON inventory_batch(clinic_id);
CREATE INDEX idx_batch_item   ON inventory_batch(clinic_id, inventory_item_id);
CREATE INDEX idx_batch_expiry ON inventory_batch(clinic_id, expiry_date);

-- RLS
ALTER TABLE inventory_batch ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON inventory_batch
    USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);
GRANT ALL ON inventory_batch TO vetapp_user;

-- Trigger za updated_at
CREATE TRIGGER trg_inventory_batch_updated_at
    BEFORE UPDATE ON inventory_batch
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- 3) batch_id na inventory_transaction (nullable — stari artikli bez lot tracking-a)
ALTER TABLE inventory_transaction
    ADD COLUMN batch_id UUID REFERENCES inventory_batch(id);

CREATE INDEX idx_inv_transaction_batch
    ON inventory_transaction(batch_id)
    WHERE deleted = false;
