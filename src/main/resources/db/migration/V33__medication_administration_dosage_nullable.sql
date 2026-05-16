-- V33__medication_administration_dosage_nullable.sql
-- Dosage polje na medication_administration postaje opciono.
-- Razlog: u praksi se često aplikuju fiksne ampule/doze pa polje
-- usporava POS-style bulk unos (BulkAdministerMedicationsModal).
-- Backward compat: postojeći zapisi sa popunjenom dozom rade isto.

ALTER TABLE medication_administration
    ALTER COLUMN dosage DROP NOT NULL;