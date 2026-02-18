-- ============================================================================
-- VetClinic - V1: Initial Schema
-- Multi-tenant veterinary clinic management system
-- ============================================================================

-- ============================================================================
-- 0. EXTENSIONS & UTILITY FUNCTIONS
-- ============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- ============================================================================
-- 1. CLINIC
-- ============================================================================
CREATE TABLE clinic (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(200)    NOT NULL,
    tax_id          VARCHAR(50)     UNIQUE,
    email           VARCHAR(150),
    phone           VARCHAR(30),
    address         TEXT,
    city            VARCHAR(100),
    country         VARCHAR(100)    DEFAULT 'Serbia',
    logo_url        VARCHAR(500),
    subscription_plan VARCHAR(30)   NOT NULL DEFAULT 'BASIC'
                        CHECK (subscription_plan IN ('BASIC', 'STANDARD', 'PREMIUM')),
    subscription_expires_at TIMESTAMPTZ,
    active          BOOLEAN         NOT NULL DEFAULT true,
    settings        JSONB           NOT NULL DEFAULT '{}',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0
);

CREATE TRIGGER trg_clinic_updated_at
    BEFORE UPDATE ON clinic
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 2. CLINIC_LOCATION
-- ============================================================================
CREATE TABLE clinic_location (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    name            VARCHAR(200)    NOT NULL,
    address         TEXT,
    city            VARCHAR(100),
    phone           VARCHAR(30),
    email           VARCHAR(150),
    is_main         BOOLEAN         NOT NULL DEFAULT false,
    active          BOOLEAN         NOT NULL DEFAULT true,
    working_hours   JSONB,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_clinic_location_clinic ON clinic_location(clinic_id);
CREATE INDEX idx_clinic_location_active ON clinic_location(clinic_id, active);

CREATE TRIGGER trg_clinic_location_updated_at
    BEFORE UPDATE ON clinic_location
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 3. ROLE
-- ============================================================================
CREATE TABLE role (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    name            VARCHAR(50)     NOT NULL,
    permissions     JSONB           NOT NULL DEFAULT '[]',
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0,

    UNIQUE (clinic_id, name)
);

CREATE TRIGGER trg_role_updated_at
    BEFORE UPDATE ON role
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 4. USERS
-- ============================================================================
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    role_id         UUID            NOT NULL REFERENCES role(id),
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(150)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    phone           VARCHAR(30),
    license_number  VARCHAR(50),
    specialization  VARCHAR(100),
    active          BOOLEAN         NOT NULL DEFAULT true,
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0,

    UNIQUE (clinic_id, email)
);

CREATE INDEX idx_users_clinic_active ON users(clinic_id, active);
CREATE INDEX idx_users_clinic_role ON users(clinic_id, role_id);

CREATE TRIGGER trg_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 5. USER_LOCATION
-- ============================================================================
CREATE TABLE user_location (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    user_id         UUID            NOT NULL REFERENCES users(id),
    location_id     UUID            NOT NULL REFERENCES clinic_location(id),
    is_primary      BOOLEAN         NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0,

    UNIQUE (user_id, location_id)
);

CREATE TRIGGER trg_user_location_updated_at
    BEFORE UPDATE ON user_location
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 6. SPECIES
-- ============================================================================
CREATE TABLE species (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    name            VARCHAR(100)    NOT NULL,
    active          BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0,

    UNIQUE (clinic_id, name)
);

CREATE TRIGGER trg_species_updated_at
    BEFORE UPDATE ON species
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 7. BREED
-- ============================================================================
CREATE TABLE breed (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    species_id      UUID            NOT NULL REFERENCES species(id),
    name            VARCHAR(100)    NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0,

    UNIQUE (species_id, name)
);

CREATE TRIGGER trg_breed_updated_at
    BEFORE UPDATE ON breed
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 8. OWNER
-- ============================================================================
CREATE TABLE owner (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    email           VARCHAR(150),
    phone           VARCHAR(30)     NOT NULL,
    address         TEXT,
    city            VARCHAR(100),
    personal_id     VARCHAR(30),
    note            TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_owner_clinic_name ON owner(clinic_id, last_name);
CREATE INDEX idx_owner_clinic_phone ON owner(clinic_id, phone);
CREATE INDEX idx_owner_clinic_email ON owner(clinic_id, email);

CREATE TRIGGER trg_owner_updated_at
    BEFORE UPDATE ON owner
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 9. PET
-- ============================================================================
CREATE TABLE pet (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    owner_id        UUID            NOT NULL REFERENCES owner(id),
    species_id      UUID            REFERENCES species(id),
    breed_id        UUID            REFERENCES breed(id),
    name            VARCHAR(100)    NOT NULL,
    date_of_birth   DATE,
    gender          VARCHAR(10)     CHECK (gender IN ('MALE', 'FEMALE', 'UNKNOWN')),
    color           VARCHAR(50),
    weight_kg       DECIMAL(6,2),
    microchip_number VARCHAR(50),
    is_neutered     BOOLEAN         NOT NULL DEFAULT false,
    is_deceased     BOOLEAN         NOT NULL DEFAULT false,
    deceased_at     DATE,
    allergies       TEXT,
    note            TEXT,
    photo_url       VARCHAR(500),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_pet_clinic_owner ON pet(clinic_id, owner_id);
CREATE INDEX idx_pet_clinic_microchip ON pet(clinic_id, microchip_number);
CREATE INDEX idx_pet_clinic_name ON pet(clinic_id, name);

CREATE TRIGGER trg_pet_updated_at
    BEFORE UPDATE ON pet
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 10. APPOINTMENT
-- ============================================================================
CREATE TABLE appointment (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    location_id     UUID            NOT NULL REFERENCES clinic_location(id),
    pet_id          UUID            NOT NULL REFERENCES pet(id),
    owner_id        UUID            NOT NULL REFERENCES owner(id),
    vet_id          UUID            NOT NULL REFERENCES users(id),
    start_time      TIMESTAMPTZ     NOT NULL,
    end_time        TIMESTAMPTZ     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'SCHEDULED'
                        CHECK (status IN ('SCHEDULED', 'CONFIRMED', 'IN_PROGRESS',
                                          'COMPLETED', 'CANCELLED', 'NO_SHOW')),
    type            VARCHAR(30)     NOT NULL
                        CHECK (type IN ('CHECKUP', 'VACCINATION', 'SURGERY',
                                        'EMERGENCY', 'FOLLOW_UP', 'GROOMING')),
    reason          TEXT,
    notes           TEXT,
    follow_up_to    UUID            REFERENCES appointment(id),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0,

    CONSTRAINT chk_appointment_time CHECK (end_time > start_time)
);

CREATE INDEX idx_appointment_clinic_time ON appointment(clinic_id, start_time);
CREATE INDEX idx_appointment_vet_time ON appointment(clinic_id, vet_id, start_time);
CREATE INDEX idx_appointment_pet ON appointment(clinic_id, pet_id);
CREATE INDEX idx_appointment_status ON appointment(clinic_id, status);
CREATE INDEX idx_appointment_location_time ON appointment(clinic_id, location_id, start_time);

CREATE TRIGGER trg_appointment_updated_at
    BEFORE UPDATE ON appointment
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 11. APPOINTMENT_STATUS_HISTORY
-- ============================================================================
CREATE TABLE appointment_status_history (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    appointment_id  UUID            NOT NULL REFERENCES appointment(id),
    from_status     VARCHAR(20),
    to_status       VARCHAR(20)     NOT NULL,
    changed_by      UUID            REFERENCES users(id),
    note            TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_appt_status_history ON appointment_status_history(appointment_id, created_at);

CREATE TRIGGER trg_appt_status_history_updated_at
    BEFORE UPDATE ON appointment_status_history
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 12. MEDICAL_RECORD
-- ============================================================================
CREATE TABLE medical_record (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    appointment_id  UUID            UNIQUE REFERENCES appointment(id),
    pet_id          UUID            NOT NULL REFERENCES pet(id),
    vet_id          UUID            NOT NULL REFERENCES users(id),
    symptoms        TEXT,
    diagnosis       TEXT,
    examination_notes TEXT,
    weight_kg       DECIMAL(6,2),
    temperature_c   DECIMAL(4,1),
    heart_rate      INTEGER,
    follow_up_recommended BOOLEAN   NOT NULL DEFAULT false,
    follow_up_date  DATE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_medical_record_pet ON medical_record(clinic_id, pet_id, created_at);
CREATE INDEX idx_medical_record_appointment ON medical_record(clinic_id, appointment_id);

CREATE TRIGGER trg_medical_record_updated_at
    BEFORE UPDATE ON medical_record
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 13. SERVICE
-- ============================================================================
CREATE TABLE service (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    category        VARCHAR(50)     NOT NULL
                        CHECK (category IN ('EXAMINATION', 'SURGERY', 'VACCINATION',
                                            'LAB', 'DENTAL', 'GROOMING', 'OTHER')),
    name            VARCHAR(200)    NOT NULL,
    description     TEXT,
    price           DECIMAL(12,2)   NOT NULL,
    tax_rate        DECIMAL(5,2)    NOT NULL DEFAULT 20.00,
    duration_minutes INTEGER,
    active          BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0,

    UNIQUE (clinic_id, name)
);

CREATE INDEX idx_service_clinic_category ON service(clinic_id, category, active);

CREATE TRIGGER trg_service_updated_at
    BEFORE UPDATE ON service
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 14. TREATMENT
-- ============================================================================
CREATE TABLE treatment (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    medical_record_id UUID          NOT NULL REFERENCES medical_record(id),
    service_id      UUID            REFERENCES service(id),
    vet_id          UUID            NOT NULL REFERENCES users(id),
    name            VARCHAR(200)    NOT NULL,
    description     TEXT,
    tooth_chart     JSONB,
    result          TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_treatment_medical_record ON treatment(clinic_id, medical_record_id);

CREATE TRIGGER trg_treatment_updated_at
    BEFORE UPDATE ON treatment
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 15. VACCINATION
-- ============================================================================
CREATE TABLE vaccination (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    pet_id          UUID            NOT NULL REFERENCES pet(id),
    medical_record_id UUID          REFERENCES medical_record(id),
    vet_id          UUID            NOT NULL REFERENCES users(id),
    vaccine_name    VARCHAR(200)    NOT NULL,
    batch_number    VARCHAR(100),
    manufacturer    VARCHAR(200),
    administered_at TIMESTAMPTZ     NOT NULL,
    valid_until     DATE,
    next_due_date   DATE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_vaccination_pet ON vaccination(clinic_id, pet_id);
CREATE INDEX idx_vaccination_due ON vaccination(clinic_id, next_due_date);

CREATE TRIGGER trg_vaccination_updated_at
    BEFORE UPDATE ON vaccination
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 16. PRESCRIPTION
-- ============================================================================
CREATE TABLE prescription (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    medical_record_id UUID          NOT NULL REFERENCES medical_record(id),
    pet_id          UUID            NOT NULL REFERENCES pet(id),
    vet_id          UUID            NOT NULL REFERENCES users(id),
    medication_name VARCHAR(200)    NOT NULL,
    dosage          VARCHAR(100)    NOT NULL,
    frequency       VARCHAR(100)    NOT NULL,
    duration_days   INTEGER,
    start_date      DATE            NOT NULL,
    end_date        DATE,
    instructions    TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_prescription_pet ON prescription(clinic_id, pet_id);
CREATE INDEX idx_prescription_record ON prescription(clinic_id, medical_record_id);

CREATE TRIGGER trg_prescription_updated_at
    BEFORE UPDATE ON prescription
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 17. INVOICE
-- ============================================================================
CREATE TABLE invoice (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    appointment_id  UUID            REFERENCES appointment(id),
    owner_id        UUID            NOT NULL REFERENCES owner(id),
    location_id     UUID            REFERENCES clinic_location(id),
    invoice_number  VARCHAR(50)     NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'DRAFT'
                        CHECK (status IN ('DRAFT', 'ISSUED', 'PAID', 'PARTIALLY_PAID',
                                          'OVERDUE', 'CANCELLED', 'REFUNDED')),
    issued_at       TIMESTAMPTZ,
    due_date        DATE,
    subtotal        DECIMAL(12,2)   NOT NULL DEFAULT 0,
    tax_amount      DECIMAL(12,2)   NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12,2)   NOT NULL DEFAULT 0,
    total           DECIMAL(12,2)   NOT NULL DEFAULT 0,
    currency        VARCHAR(3)      NOT NULL DEFAULT 'RSD',
    note            TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0,

    UNIQUE (clinic_id, invoice_number)
);

CREATE INDEX idx_invoice_clinic_owner ON invoice(clinic_id, owner_id);
CREATE INDEX idx_invoice_clinic_status ON invoice(clinic_id, status);
CREATE INDEX idx_invoice_clinic_issued ON invoice(clinic_id, issued_at);

CREATE TRIGGER trg_invoice_updated_at
    BEFORE UPDATE ON invoice
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 18. INVOICE_ITEM
-- ============================================================================
CREATE TABLE invoice_item (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    invoice_id      UUID            NOT NULL REFERENCES invoice(id) ON DELETE CASCADE,
    service_id      UUID            REFERENCES service(id),
    description     VARCHAR(300)    NOT NULL,
    quantity        DECIMAL(8,2)    NOT NULL DEFAULT 1,
    unit_price      DECIMAL(12,2)   NOT NULL,
    tax_rate        DECIMAL(5,2)    NOT NULL DEFAULT 20.00,
    discount_percent DECIMAL(5,2)   NOT NULL DEFAULT 0,
    line_total      DECIMAL(12,2)   NOT NULL,
    sort_order      INTEGER         NOT NULL DEFAULT 0,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_invoice_item_invoice ON invoice_item(invoice_id);

CREATE TRIGGER trg_invoice_item_updated_at
    BEFORE UPDATE ON invoice_item
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 19. PAYMENT
-- ============================================================================
CREATE TABLE payment (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    invoice_id      UUID            NOT NULL REFERENCES invoice(id),
    amount          DECIMAL(12,2)   NOT NULL,
    method          VARCHAR(20)     NOT NULL
                        CHECK (method IN ('CASH', 'CARD', 'TRANSFER', 'OTHER')),
    paid_at         TIMESTAMPTZ     NOT NULL,
    reference_number VARCHAR(100),
    note            TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_payment_invoice ON payment(clinic_id, invoice_id);

CREATE TRIGGER trg_payment_updated_at
    BEFORE UPDATE ON payment
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 20. INVENTORY_ITEM
-- ============================================================================
CREATE TABLE inventory_item (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    location_id     UUID            REFERENCES clinic_location(id),
    name            VARCHAR(200)    NOT NULL,
    sku             VARCHAR(50),
    category        VARCHAR(50)     NOT NULL
                        CHECK (category IN ('MEDICATION', 'SUPPLY', 'EQUIPMENT')),
    quantity_on_hand DECIMAL(10,2)  NOT NULL DEFAULT 0,
    unit            VARCHAR(20),
    reorder_level   DECIMAL(10,2),
    cost_price      DECIMAL(12,2),
    sell_price      DECIMAL(12,2),
    expiry_date     DATE,
    active          BOOLEAN         NOT NULL DEFAULT true,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_inventory_clinic_category ON inventory_item(clinic_id, category);
CREATE INDEX idx_inventory_clinic_name ON inventory_item(clinic_id, name);
CREATE INDEX idx_inventory_expiry ON inventory_item(clinic_id, expiry_date);

CREATE TRIGGER trg_inventory_item_updated_at
    BEFORE UPDATE ON inventory_item
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 21. INVENTORY_TRANSACTION
-- ============================================================================
CREATE TABLE inventory_transaction (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    inventory_item_id UUID          NOT NULL REFERENCES inventory_item(id),
    type            VARCHAR(20)     NOT NULL
                        CHECK (type IN ('IN', 'OUT', 'ADJUSTMENT', 'EXPIRED')),
    quantity        DECIMAL(10,2)   NOT NULL,
    reference_type  VARCHAR(30),
    reference_id    UUID,
    performed_by    UUID            REFERENCES users(id),
    note            TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_inv_transaction_item ON inventory_transaction(clinic_id, inventory_item_id);

CREATE TRIGGER trg_inventory_transaction_updated_at
    BEFORE UPDATE ON inventory_transaction
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 22. DOCUMENT
-- ============================================================================
CREATE TABLE document (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    pet_id          UUID            REFERENCES pet(id),
    medical_record_id UUID          REFERENCES medical_record(id),
    uploaded_by     UUID            NOT NULL REFERENCES users(id),
    file_name       VARCHAR(300)    NOT NULL,
    file_type       VARCHAR(50)     NOT NULL
                        CHECK (file_type IN ('IMAGE', 'PDF', 'LAB_RESULT', 'XRAY', 'OTHER')),
    mime_type       VARCHAR(100),
    file_size_bytes BIGINT,
    storage_path    VARCHAR(500)    NOT NULL,
    description     TEXT,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_document_pet ON document(clinic_id, pet_id);
CREATE INDEX idx_document_record ON document(clinic_id, medical_record_id);

CREATE TRIGGER trg_document_updated_at
    BEFORE UPDATE ON document
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 23. NOTIFICATION
-- ============================================================================
CREATE TABLE notification (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            NOT NULL REFERENCES clinic(id),
    recipient_type  VARCHAR(20)     NOT NULL CHECK (recipient_type IN ('OWNER', 'USER')),
    recipient_id    UUID            NOT NULL,
    type            VARCHAR(30)     NOT NULL
                        CHECK (type IN ('APPOINTMENT_REMINDER', 'VACCINATION_DUE',
                                        'INVOICE_DUE', 'FOLLOW_UP')),
    channel         VARCHAR(10)     NOT NULL CHECK (channel IN ('EMAIL', 'SMS', 'PUSH')),
    title           VARCHAR(200)    NOT NULL,
    message         TEXT            NOT NULL,
    scheduled_at    TIMESTAMPTZ,
    sent_at         TIMESTAMPTZ,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING'
                        CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'CANCELLED')),
    reference_type  VARCHAR(30),
    reference_id    UUID,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    deleted         BOOLEAN         NOT NULL DEFAULT false,
    deleted_at      TIMESTAMPTZ,
    version         INTEGER         NOT NULL DEFAULT 0
);

CREATE INDEX idx_notification_scheduled ON notification(clinic_id, status, scheduled_at);
CREATE INDEX idx_notification_recipient ON notification(clinic_id, recipient_type, recipient_id);

CREATE TRIGGER trg_notification_updated_at
    BEFORE UPDATE ON notification
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- ============================================================================
-- 24. AUDIT_LOG
-- ============================================================================
CREATE TABLE audit_log (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID            REFERENCES clinic(id),
    user_id         UUID            REFERENCES users(id),
    action          VARCHAR(20)     NOT NULL
                        CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'LOGIN', 'LOGOUT')),
    entity_type     VARCHAR(50)     NOT NULL,
    entity_id       UUID,
    old_values      JSONB,
    new_values      JSONB,
    ip_address      VARCHAR(45),
    user_agent      VARCHAR(500),
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_entity ON audit_log(clinic_id, entity_type, entity_id);
CREATE INDEX idx_audit_user ON audit_log(clinic_id, user_id);
CREATE INDEX idx_audit_created ON audit_log(clinic_id, created_at);

-- ============================================================================
-- 25. REFRESH_TOKEN
-- ============================================================================
CREATE TABLE refresh_token (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID            NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token           VARCHAR(500)    NOT NULL UNIQUE,
    expires_at      TIMESTAMPTZ     NOT NULL,
    revoked         BOOLEAN         NOT NULL DEFAULT false,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_token_user ON refresh_token(user_id);
