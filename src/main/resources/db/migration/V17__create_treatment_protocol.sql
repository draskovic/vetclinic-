CREATE TABLE treatment_protocol (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id UUID NOT NULL REFERENCES clinic(id),
    diagnosis_id UUID REFERENCES diagnosis(id),
    name VARCHAR(300) NOT NULL,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMPTZ,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_protocol_clinic_name
    ON treatment_protocol(clinic_id, name) WHERE deleted = false;
CREATE INDEX idx_protocol_clinic_diagnosis
    ON treatment_protocol(clinic_id, diagnosis_id) WHERE deleted = false;

CREATE TRIGGER set_updated_at BEFORE UPDATE ON treatment_protocol
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE treatment_protocol ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON treatment_protocol
    USING (clinic_id = current_setting('app.current_clinic_id')::UUID);
GRANT ALL ON treatment_protocol TO vetapp_user;

-- Stavke protokola (usluge u protokolu)
CREATE TABLE treatment_protocol_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id UUID NOT NULL REFERENCES clinic(id),
    protocol_id UUID NOT NULL REFERENCES treatment_protocol(id),
    service_id UUID NOT NULL REFERENCES service(id),
    quantity INTEGER NOT NULL DEFAULT 1,
    notes TEXT,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMPTZ,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_protocol_item_protocol
    ON treatment_protocol_item(protocol_id) WHERE deleted = false;

CREATE TRIGGER set_updated_at BEFORE UPDATE ON treatment_protocol_item
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE treatment_protocol_item ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON treatment_protocol_item
    USING (clinic_id = current_setting('app.current_clinic_id')::UUID);
GRANT ALL ON treatment_protocol_item TO vetapp_user;
