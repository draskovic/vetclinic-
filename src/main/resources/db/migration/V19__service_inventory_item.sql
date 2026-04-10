-- ============================================================
-- V19: service_inventory_item — veza usluga ↔ inventar artikal
-- ============================================================

CREATE TABLE service_inventory_item (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id         UUID            NOT NULL REFERENCES clinic(id),
    service_id        UUID            NOT NULL REFERENCES service(id),
    inventory_item_id UUID            NOT NULL REFERENCES inventory_item(id),
    quantity_per_use  DECIMAL(10,2)   NOT NULL DEFAULT 1,
    created_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted           BOOLEAN         NOT NULL DEFAULT false,
    deleted_at        TIMESTAMPTZ,
    version           INTEGER         NOT NULL DEFAULT 0
);

-- Indeksi
CREATE UNIQUE INDEX idx_sii_unique
    ON service_inventory_item(clinic_id, service_id, inventory_item_id)
    WHERE deleted = false;
CREATE INDEX idx_sii_service ON service_inventory_item(clinic_id, service_id);
CREATE INDEX idx_sii_item    ON service_inventory_item(clinic_id, inventory_item_id);

-- RLS
ALTER TABLE service_inventory_item ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON service_inventory_item
    USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);
GRANT ALL ON service_inventory_item TO vetapp_user;

-- Trigger za updated_at
CREATE TRIGGER trg_sii_updated_at
    BEFORE UPDATE ON service_inventory_item
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
