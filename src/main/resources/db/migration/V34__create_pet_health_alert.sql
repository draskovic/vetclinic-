CREATE TABLE pet_health_alert (
    id                  UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id           UUID         NOT NULL REFERENCES clinic(id),
    pet_id              UUID         NOT NULL REFERENCES pet(id),
    alert_type          VARCHAR(30)  NOT NULL,
    label               VARCHAR(200) NOT NULL,
    description         TEXT,
    active              BOOLEAN      NOT NULL DEFAULT true,
    
    created_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ  NOT NULL DEFAULT now(),
    deleted             BOOLEAN      NOT NULL DEFAULT false,
    deleted_at          TIMESTAMPTZ,
    version             INTEGER      NOT NULL DEFAULT 0,
    CONSTRAINT pet_health_alert_type_check
        CHECK (alert_type IN ('ALLERGY','CHRONIC_CONDITION','SPECIAL_HANDLING','OTHER'))
);

CREATE INDEX idx_pet_health_alert_pet
    ON pet_health_alert(clinic_id, pet_id) WHERE deleted = false AND active = true;
CREATE INDEX idx_pet_health_alert_clinic
    ON pet_health_alert(clinic_id) WHERE deleted = false;

CREATE TRIGGER trg_pet_health_alert_updated_at
    BEFORE UPDATE ON pet_health_alert
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

ALTER TABLE pet_health_alert ENABLE ROW LEVEL SECURITY;
CREATE POLICY tenant_isolation ON pet_health_alert
    FOR ALL USING (clinic_id = NULLIF(current_setting('app.current_clinic_id', true), '')::uuid);

DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'vetapp_user') THEN
        EXECUTE 'GRANT ALL ON pet_health_alert TO vetapp_user';
    END IF;
END $$;

-- Data migration: prebaci postojeće pet.allergies tekstove u pet_health_alert sa type=ALLERGY
-- Flyway izvršava ceo fajl u jednoj tx-i: ako INSERT padne, CREATE TABLE se rollback-uje
-- → migration je atomski "failed", nikad "partial". Bez ON CONFLICT (UUID PK je uvek nov).
INSERT INTO pet_health_alert (clinic_id, pet_id, alert_type, label, active)
SELECT p.clinic_id, p.id, 'ALLERGY', LEFT(TRIM(p.allergies), 200), true
FROM pet p
WHERE p.allergies IS NOT NULL
  AND TRIM(p.allergies) != ''
  AND p.deleted = false;