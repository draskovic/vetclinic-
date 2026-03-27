ALTER TABLE pet ADD COLUMN patient_code VARCHAR(20);

CREATE UNIQUE INDEX idx_pet_patient_code_clinic
    ON pet(clinic_id, patient_code)
    WHERE patient_code IS NOT NULL AND deleted = false;
