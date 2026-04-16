-- V22: Online booking - nova polja na appointment, booking_settings tabela, slug na clinic

-- 1. Nova polja na appointment
ALTER TABLE appointment ADD COLUMN cancellation_token VARCHAR(100);
ALTER TABLE appointment ADD COLUMN booking_source VARCHAR(20) NOT NULL DEFAULT 'CLINIC';
CREATE UNIQUE INDEX idx_appointment_cancellation_token 
    ON appointment(cancellation_token) WHERE cancellation_token IS NOT NULL;

-- 2. Booking settings po klinici (1:1 sa clinic, BEZ RLS)
CREATE TABLE booking_settings (
    id                    UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id             UUID NOT NULL UNIQUE REFERENCES clinic(id),
    enabled               BOOLEAN NOT NULL DEFAULT false,
    slot_duration_minutes INTEGER NOT NULL DEFAULT 30,
    buffer_minutes        INTEGER NOT NULL DEFAULT 0,
    max_advance_days      INTEGER NOT NULL DEFAULT 30,
    allowed_types         JSONB NOT NULL DEFAULT '["CHECKUP","VACCINATION","GROOMING"]',
    auto_confirm          BOOLEAN NOT NULL DEFAULT false,
    allow_vet_selection   BOOLEAN NOT NULL DEFAULT false,
    cancellation_hours    INTEGER NOT NULL DEFAULT 24,
    timezone              VARCHAR(50) NOT NULL DEFAULT 'Europe/Belgrade',
    created_at            TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- 3. Slug na klinici za buduce lepse URL-ove
ALTER TABLE clinic ADD COLUMN slug VARCHAR(100);
CREATE UNIQUE INDEX idx_clinic_slug ON clinic(slug) WHERE slug IS NOT NULL;
