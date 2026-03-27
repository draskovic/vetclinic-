ALTER TABLE pet ADD COLUMN legacy_code VARCHAR(50); 

CREATE INDEX idx_pet_legacy_code_clinic                                                                                      
      ON pet(clinic_id, legacy_code) 
      WHERE legacy_code IS NOT NULL AND deleted = false;