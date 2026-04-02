ALTER TABLE medical_record ADD COLUMN record_code VARCHAR(20);

CREATE UNIQUE INDEX idx_medical_record_code_unique 
ON medical_record (clinic_id, record_code) 
WHERE record_code IS NOT NULL AND deleted = false;
