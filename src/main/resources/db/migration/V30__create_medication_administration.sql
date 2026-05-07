CREATE TABLE medication_administration (
    id                 UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id          UUID         NOT NULL REFERENCES clinic(id),
    medical_record_id  UUID         NOT NULL REFERENCES medical_record(id),
    pet_id             UUID         NOT NULL REFERENCES pet(id),
    vet_id             UUID         NOT NULL REFERENCES users(id),
    inventory_item_id  UUID         REFERENCES inventory_item(id),
    medication_name    VARCHAR(200) NOT NULL,
    dosage             VARCHAR(100) NOT NULL,
    route              VARCHAR(20),
    administered_date  DATE         NOT NULL,
    instructions       TEXT,
    created_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at         TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted            BOOLEAN      NOT NULL DEFAULT false,
    deleted_at         TIMESTAMPTZ,
    version            INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT medication_administration_route_check
        CHECK (route IS NULL OR route IN ('IV','IM','SC','PO','TOPICAL','INHALATION'))
);

CREATE INDEX idx_med_admin_pet
    ON medication_administration(clinic_id, pet_id) WHERE deleted = false;
CREATE INDEX idx_med_admin_record
    ON medication_administration(clinic_id, medical_record_id) WHERE deleted = false;
CREATE INDEX idx_med_admin_inventory_item
    ON medication_administration(inventory_item_id) WHERE inventory_item_id IS NOT NULL;

CREATE TRIGGER trg_med_admin_updated_at
    BEFORE UPDATE ON medication_administration
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE medication_administration ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON medication_administration
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'vetapp_user') THEN
        EXECUTE 'GRANT ALL ON medication_administration TO vetapp_user';
    END IF;
END $$;