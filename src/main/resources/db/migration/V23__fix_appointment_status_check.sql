-- Ukloni stari status CHECK constraint
ALTER TABLE appointment DROP CONSTRAINT appointment_status_check;

-- Dodaj novi sa PENDING
ALTER TABLE appointment ADD CONSTRAINT appointment_status_check 
    CHECK (status IN ('PENDING', 'SCHEDULED', 'CONFIRMED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'NO_SHOW'));
