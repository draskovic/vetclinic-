ALTER TABLE owner ADD COLUMN client_code VARCHAR(20);

CREATE UNIQUE INDEX idx_owner_client_code_clinic
    ON owner(clinic_id, client_code)
    WHERE client_code IS NOT NULL AND deleted = false;
