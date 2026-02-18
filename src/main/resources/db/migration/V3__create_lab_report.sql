-- ============================================================================
-- VetClinic - V3: Create lab_report table
-- ============================================================================

CREATE TABLE lab_report (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id           UUID NOT NULL REFERENCES clinic(id),
    pet_id              UUID NOT NULL REFERENCES pet(id),
    vet_id              UUID NOT NULL REFERENCES users(id),
    medical_record_id   UUID REFERENCES medical_record(id),
    report_number       VARCHAR(50) NOT NULL,
    analysis_type       VARCHAR(200) NOT NULL,
    laboratory_name     VARCHAR(200),
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                            CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED')),
    requested_at        DATE NOT NULL DEFAULT CURRENT_DATE,
    completed_at        DATE,
    result_summary      TEXT,
    is_abnormal         BOOLEAN NOT NULL DEFAULT false,
    notes               TEXT,
    file_name           VARCHAR(300),
    mime_type           VARCHAR(100) DEFAULT 'application/pdf',
    file_size_bytes     BIGINT,
    storage_path        VARCHAR(500),
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    deleted             BOOLEAN NOT NULL DEFAULT false,
    deleted_at          TIMESTAMPTZ,
    version             INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT uq_lab_report_number_clinic UNIQUE (clinic_id, report_number)
);

CREATE INDEX idx_lab_report_clinic_id ON lab_report(clinic_id);
CREATE INDEX idx_lab_report_pet_id ON lab_report(pet_id);
CREATE INDEX idx_lab_report_vet_id ON lab_report(vet_id);
CREATE INDEX idx_lab_report_medical_record_id ON lab_report(medical_record_id);
CREATE INDEX idx_lab_report_status ON lab_report(clinic_id, status);
CREATE INDEX idx_lab_report_requested_at ON lab_report(clinic_id, requested_at DESC);
CREATE INDEX idx_lab_report_deleted ON lab_report(clinic_id, deleted);

CREATE TRIGGER trg_lab_report_updated_at
    BEFORE UPDATE ON lab_report
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- RLS
ALTER TABLE lab_report ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON lab_report
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

GRANT SELECT, INSERT, UPDATE, DELETE ON lab_report TO vetapp_user;
