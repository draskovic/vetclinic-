-- ============================================================
-- V37: (A) guard duplikata inventory_item + unique (clinic, product, location)
--      (B) medical_record.location_id (kolona + FK + backfill + indeks)
-- ============================================================
-- Flyway izvršava ceo fajl u JEDNOJ tx-i (atomski).

-- ------------------------------------------------------------
-- A1) Dedup inventory_item po (clinic_id, product_id, location_id)
--     NULL location_id se grupiše (GROUP BY/PARTITION tretira NULL-ove kao jednake).
--     Survivor = najstariji red. Prazan duplikat → soft-delete; duplikat sa
--     stanjem/referencama → RAISE (ručno razrešiti; u praksi se ne dešava).
-- ------------------------------------------------------------
DO $$
DECLARE
    blocking int;
BEGIN
    CREATE TEMP TABLE _dedup_loser ON COMMIT DROP AS
    SELECT id AS loser_id
    FROM (
        SELECT id, ROW_NUMBER() OVER (
                   PARTITION BY clinic_id, product_id, location_id
                   ORDER BY created_at, id) AS rn
        FROM inventory_item
        WHERE deleted = false
    ) r
    WHERE r.rn > 1;

    SELECT COUNT(*) INTO blocking
    FROM _dedup_loser l
    WHERE EXISTS (SELECT 1 FROM inventory_batch b
                  WHERE b.inventory_item_id = l.loser_id
                    AND b.deleted = false AND b.quantity_on_hand <> 0)
       OR EXISTS (SELECT 1 FROM inventory_transaction t
                  WHERE t.inventory_item_id = l.loser_id)
       OR EXISTS (SELECT 1 FROM invoice_item ii
                  WHERE ii.inventory_item_id = l.loser_id)
       OR EXISTS (SELECT 1 FROM medication_administration ma
                  WHERE ma.inventory_item_id = l.loser_id);

    IF blocking > 0 THEN
        RAISE EXCEPTION
          'V37: % duplikat(a) inventory_item nosi stanje/reference — ručno razrešiti pre migracije', blocking;
    END IF;

    -- prazni duplikati: soft-delete item + njegov (prazan) DEFAULT lot
    UPDATE inventory_batch b SET deleted = true, deleted_at = NOW()
    FROM _dedup_loser l WHERE b.inventory_item_id = l.loser_id;

    UPDATE inventory_item i SET deleted = true, deleted_at = NOW()
    FROM _dedup_loser l WHERE i.id = l.loser_id;
END $$;

-- ------------------------------------------------------------
-- A2) Unique guard: jedan inventory_item po (clinic, product, location)
--     NULLS NOT DISTINCT → blokira i dva NULL-location reda za isti proizvod (PG 15+).
-- ------------------------------------------------------------
CREATE UNIQUE INDEX idx_inventory_item_unique_product_location
    ON inventory_item (clinic_id, product_id, location_id)
    NULLS NOT DISTINCT
    WHERE deleted = false;

-- ------------------------------------------------------------
-- B) medical_record.location_id (lokacija pružanja intervencije)
-- ------------------------------------------------------------
ALTER TABLE medical_record ADD COLUMN location_id UUID;

-- backfill 1: intervencije sa terminom → lokacija termina (appointment.location_id je NOT NULL)
UPDATE medical_record mr
SET location_id = a.location_id
FROM appointment a
WHERE a.id = mr.appointment_id AND mr.appointment_id IS NOT NULL;

-- backfill 2: intervencije bez termina → glavna lokacija klinike
UPDATE medical_record mr
SET location_id = (
    SELECT cl.id FROM clinic_location cl
    WHERE cl.clinic_id = mr.clinic_id
      AND cl.is_main = true AND cl.deleted = false
    ORDER BY cl.created_at LIMIT 1)
WHERE mr.location_id IS NULL;

-- kolona ostaje NULLABLE (klinika bez glavne lokacije → null; dedukcija ima fallback lanac)
ALTER TABLE medical_record
    ADD CONSTRAINT fk_medical_record_location
    FOREIGN KEY (location_id) REFERENCES clinic_location(id);

CREATE INDEX idx_medical_record_location
    ON medical_record(clinic_id, location_id) WHERE deleted = false;