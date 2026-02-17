-- ============================================================
-- Lab Report table — čuvanje laboratorijskih analiza
-- Run in pgAdmin on vetapp database
-- ============================================================

-- 1. Enum za status lab izveštaja
CREATE TYPE lab_report_status AS ENUM ('PENDING', 'COMPLETED', 'CANCELLED');

-- 2. Tabela lab_report
CREATE TABLE lab_report (
    -- BaseEntity kolone (isti pattern kao sve ostale tabele)
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID NOT NULL REFERENCES clinic(id),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted         BOOLEAN NOT NULL DEFAULT false,
    deleted_at      TIMESTAMP WITH TIME ZONE,
    version         INTEGER NOT NULL DEFAULT 0,

    -- Identifikacija izveštaja
    report_number       VARCHAR(50) NOT NULL,           -- Broj lab. izveštaja (npr. "LAB-2025-001234")
    analysis_type       VARCHAR(200) NOT NULL,          -- Vrsta analize (npr. "Bris uha mali antibiogram")

    -- Veze sa drugim entitetima
    pet_id              UUID NOT NULL REFERENCES pet(id),               -- Ljubimac
    vet_id              UUID NOT NULL REFERENCES users(id),             -- Veterinar koji je tražio analizu
    medical_record_id   UUID REFERENCES medical_record(id),            -- Opciono — povezana intervencija

    -- Laboratorija
    laboratory_name     VARCHAR(200),                   -- Naziv laboratorije (npr. "Pasteur laboratorija")

    -- Status i datumi
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',         -- PENDING, COMPLETED, CANCELLED
    requested_at        DATE NOT NULL DEFAULT CURRENT_DATE,             -- Datum kad je analiza tražena
    completed_at        DATE,                                           -- Datum kad je rezultat stigao

    -- Rezultati
    result_summary      TEXT,                           -- Kratak tekstualni opis rezultata
    is_abnormal         BOOLEAN NOT NULL DEFAULT false, -- Flag za nenormalne nalaze
    notes               TEXT,                           -- Dodatne napomene

    -- PDF fajl metapodaci (fajl se čuva na file sistemu, ovde samo putanja)
    file_name           VARCHAR(300),                   -- Originalno ime fajla (npr. "lab-report-001234.pdf")
    mime_type           VARCHAR(100) DEFAULT 'application/pdf',
    file_size_bytes     BIGINT,                         -- Veličina fajla u bajtovima
    storage_path        VARCHAR(500),                   -- Putanja na file sistemu (npr. "uploads/lab-reports/clinic-uuid/2025/001234.pdf")

    -- Constraints
    CONSTRAINT uq_lab_report_number_clinic UNIQUE (clinic_id, report_number)
);

-- 3. Indeksi za brzo pretraživanje
CREATE INDEX idx_lab_report_clinic_id ON lab_report(clinic_id);
CREATE INDEX idx_lab_report_pet_id ON lab_report(pet_id);
CREATE INDEX idx_lab_report_vet_id ON lab_report(vet_id);
CREATE INDEX idx_lab_report_medical_record_id ON lab_report(medical_record_id);
CREATE INDEX idx_lab_report_status ON lab_report(clinic_id, status);
CREATE INDEX idx_lab_report_requested_at ON lab_report(clinic_id, requested_at DESC);
CREATE INDEX idx_lab_report_deleted ON lab_report(clinic_id, deleted);

-- 4. RLS (Row-Level Security) — isti pattern kao ostale tabele
ALTER TABLE lab_report ENABLE ROW LEVEL SECURITY;

CREATE POLICY tenant_isolation ON lab_report
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

-- 5. SUPER_ADMIN pristup (ako postoji ta politika na drugim tabelama — proveri)
-- CREATE POLICY super_admin_full_access ON lab_report
--     FOR ALL USING (true);

-- 6. Grant privilegije za vetapp_user (ako koristite RLS usera)
GRANT SELECT, INSERT, UPDATE, DELETE ON lab_report TO vetapp_user;

-- ============================================================
-- Verifikacija: proveri da je tabela kreirana
-- ============================================================
-- SELECT column_name, data_type, is_nullable, column_default
-- FROM information_schema.columns
-- WHERE table_name = 'lab_report'
-- ORDER BY ordinal_position;
