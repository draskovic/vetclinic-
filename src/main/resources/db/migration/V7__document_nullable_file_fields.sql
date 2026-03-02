-- V7__document_nullable_file_fields.sql
-- Dozvoli kreiranje dokumenta bez fajla (upload se radi naknadno)

ALTER TABLE document ALTER COLUMN file_name DROP NOT NULL;
ALTER TABLE document ALTER COLUMN storage_path DROP NOT NULL;
