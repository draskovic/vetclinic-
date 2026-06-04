-- V35__add_treatment_billing_fields.sql
-- Klinička linija (treatment) postaje izvor istine za finansijske atribute stavke:
-- količina, jedinična cena (override) i popust. Faktura postaje ogledalo (model 1b).
--   1. quantity         NUMERIC(8,2)  NOT NULL DEFAULT 1   (broj jedinica usluge)
--   2. unit_price       NUMERIC(12,2) NULL                 (override; NULL = cena iz kataloga usluge)
--   3. discount_percent NUMERIC(5,2)  NOT NULL DEFAULT 0   (popust po liniji)
-- Precision/scale usklađen sa invoice_item (8,2 / 12,2 / 5,2).
-- Bez CHECK-a, bez enuma; treatment se NE mapira preko native RETURNS TABLE funkcije → čist ALTER.

ALTER TABLE treatment ADD COLUMN quantity NUMERIC(8,2) NOT NULL DEFAULT 1;
ALTER TABLE treatment ADD COLUMN unit_price NUMERIC(12,2);
ALTER TABLE treatment ADD COLUMN discount_percent NUMERIC(5,2) NOT NULL DEFAULT 0;