-- V31__add_tax_rate_codebook.sql
-- Uvodi globalan šifarnik poreskih stopa (per-zemlja) i refaktoriše
-- postojeće DECIMAL kolone tax_rate na FK reference + snapshot polja.

-- 1) Tabela šifarnika (globalna, BEZ clinic_id, BEZ RLS — javni podaci)
CREATE TABLE tax_rate (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    country_code VARCHAR(2)   NOT NULL,
    label        VARCHAR(2)   NOT NULL,
    percent      DECIMAL(5,2) NOT NULL,
    description  VARCHAR(200) NOT NULL,
    active       BOOLEAN      NOT NULL DEFAULT true,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted      BOOLEAN      NOT NULL DEFAULT false,
    CONSTRAINT uk_tax_rate_country_label UNIQUE (country_code, label)
);

CREATE INDEX idx_tax_rate_country_active
    ON tax_rate(country_code)
    WHERE deleted = false AND active = true;

-- 2) Seed za Srbiju (4 oznake)
INSERT INTO tax_rate (country_code, label, percent, description) VALUES
    ('RS', 'А', 0.00,  'Bez PDV-a (nije obveznik)'),
    ('RS', 'Ђ', 20.00, 'Opšta stopa PDV'),
    ('RS', 'Е', 10.00, 'Posebna stopa PDV'),
    ('RS', 'Г', 0.00,  'Oslobođeno / neoporezivo');

-- 3) service.tax_rate_id (FK) + backfill iz postojeće DECIMAL kolone
ALTER TABLE service ADD COLUMN tax_rate_id UUID;
ALTER TABLE service ADD CONSTRAINT fk_service_tax_rate
    FOREIGN KEY (tax_rate_id) REFERENCES tax_rate(id);

UPDATE service s
SET tax_rate_id = tr.id
FROM clinic c, tax_rate tr
WHERE c.id = s.clinic_id
  AND tr.country_code = 'RS'
  AND tr.label = CASE
        WHEN s.tax_rate = 20.00 THEN 'Ђ'
        WHEN s.tax_rate = 10.00 THEN 'Е'
        WHEN s.tax_rate = 0.00 AND COALESCE(c.vat_payer, false) = false THEN 'А'
        WHEN s.tax_rate = 0.00 AND c.vat_payer = true                    THEN 'Г'
        ELSE 'Ђ'
      END;

ALTER TABLE service ALTER COLUMN tax_rate_id SET NOT NULL;
ALTER TABLE service DROP COLUMN tax_rate;

-- 4) invoice_item: snapshot polja (id + label + percent) — fakture su immutable istorija
ALTER TABLE invoice_item ADD COLUMN tax_rate_id      UUID;
ALTER TABLE invoice_item ADD COLUMN tax_rate_label   VARCHAR(2);
ALTER TABLE invoice_item ADD COLUMN tax_rate_percent DECIMAL(5,2);
ALTER TABLE invoice_item ADD CONSTRAINT fk_invoice_item_tax_rate
    FOREIGN KEY (tax_rate_id) REFERENCES tax_rate(id);

UPDATE invoice_item ii
SET tax_rate_percent = ii.tax_rate,
    tax_rate_label   = CASE
        WHEN ii.tax_rate = 20.00 THEN 'Ђ'
        WHEN ii.tax_rate = 10.00 THEN 'Е'
        WHEN ii.tax_rate = 0.00 AND COALESCE(c.vat_payer, false) = false THEN 'А'
        WHEN ii.tax_rate = 0.00 AND c.vat_payer = true                   THEN 'Г'
        ELSE 'Ђ'
    END,
    tax_rate_id = tr.id
FROM clinic c, tax_rate tr
WHERE c.id = ii.clinic_id
  AND tr.country_code = 'RS'
  AND tr.label = CASE
        WHEN ii.tax_rate = 20.00 THEN 'Ђ'
        WHEN ii.tax_rate = 10.00 THEN 'Е'
        WHEN ii.tax_rate = 0.00 AND COALESCE(c.vat_payer, false) = false THEN 'А'
        WHEN ii.tax_rate = 0.00 AND c.vat_payer = true                   THEN 'Г'
        ELSE 'Ђ'
      END;

ALTER TABLE invoice_item ALTER COLUMN tax_rate_id      SET NOT NULL;
ALTER TABLE invoice_item ALTER COLUMN tax_rate_label   SET NOT NULL;
ALTER TABLE invoice_item ALTER COLUMN tax_rate_percent SET NOT NULL;
ALTER TABLE invoice_item DROP COLUMN tax_rate;