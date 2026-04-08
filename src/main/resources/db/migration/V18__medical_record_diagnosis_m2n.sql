-- 1. Junction tabela za M:N vezu medical_record ↔ diagnosis
CREATE TABLE medical_record_diagnosis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id UUID NOT NULL REFERENCES clinic(id),
    medical_record_id UUID NOT NULL REFERENCES medical_record(id),
    diagnosis_id UUID NOT NULL REFERENCES diagnosis(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX idx_mrd_record_diagnosis
    ON medical_record_diagnosis(medical_record_id, diagnosis_id);
CREATE INDEX idx_mrd_clinic_record
    ON medical_record_diagnosis(clinic_id, medical_record_id);

ALTER TABLE medical_record_diagnosis ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON medical_record_diagnosis
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

DO $$ BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'vetapp_user') THEN
        EXECUTE 'GRANT ALL ON medical_record_diagnosis TO vetapp_user';
    END IF;
END $$;

-- 2. Uklanjanje stare text kolone (podaci se ne migriraju — bio slobodan tekst)
ALTER TABLE medical_record DROP COLUMN IF EXISTS diagnosis;
