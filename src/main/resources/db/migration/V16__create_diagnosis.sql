CREATE TABLE diagnosis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id UUID NOT NULL REFERENCES clinic(id),
    code VARCHAR(20),
    name VARCHAR(300) NOT NULL,
    category VARCHAR(100),
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    deleted BOOLEAN NOT NULL DEFAULT false,
    deleted_at TIMESTAMPTZ,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX idx_diagnosis_clinic_name
    ON diagnosis(clinic_id, name) WHERE deleted = false;

CREATE INDEX idx_diagnosis_clinic_code
    ON diagnosis(clinic_id, code) WHERE deleted = false;

CREATE TRIGGER set_updated_at BEFORE UPDATE ON diagnosis
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE diagnosis ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON diagnosis
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'vetapp_user') THEN
        EXECUTE 'GRANT ALL ON diagnosis TO vetapp_user';
    END IF;
END $$;

