-- Atributi klinike specifični za Srbiju i slične pravne sisteme
ALTER TABLE clinic ADD COLUMN registration_number VARCHAR(20);
ALTER TABLE clinic ADD COLUMN activity_code VARCHAR(10);
ALTER TABLE clinic ADD COLUMN bank_account VARCHAR(50);
ALTER TABLE clinic ADD COLUMN vat_payer BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE clinic ADD COLUMN veterinary_license_number VARCHAR(50);

COMMENT ON COLUMN clinic.registration_number IS 'Matični broj (8 cifara u Srbiji)';
COMMENT ON COLUMN clinic.activity_code IS 'Šifra delatnosti (npr. 7500 za veterinu u Srbiji)';
COMMENT ON COLUMN clinic.bank_account IS 'Tekući račun za uplate (obavezan na fakturama)';
COMMENT ON COLUMN clinic.vat_payer IS 'Da li je klinika u sistemu PDV-a';
COMMENT ON COLUMN clinic.veterinary_license_number IS 'Broj licence za obavljanje veterinarske delatnosti';
