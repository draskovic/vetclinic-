ALTER TABLE invoice ADD COLUMN medical_record_id UUID;

ALTER TABLE invoice ADD CONSTRAINT fk_invoice_medical_record
    FOREIGN KEY (medical_record_id) REFERENCES medical_record(id);

ALTER TABLE invoice ADD CONSTRAINT uq_invoice_medical_record_id
    UNIQUE (medical_record_id);
