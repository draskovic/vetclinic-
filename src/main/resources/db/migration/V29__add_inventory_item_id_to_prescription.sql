-- Opciona veza recepta sa lekom iz inventara (kategorija MEDICATION)
-- inventory_item_id NULL — dozvoljava recept sa slobodnim tekstom (lek koji klinika ne drži u lageru)
-- Parcijalni indeks samo nad NOT NULL vrednostima — manji indeks, efikasnija pretraga

ALTER TABLE prescription
    ADD COLUMN inventory_item_id UUID NULL;

ALTER TABLE prescription
    ADD CONSTRAINT fk_prescription_inventory_item
    FOREIGN KEY (inventory_item_id)
    REFERENCES inventory_item(id);

CREATE INDEX idx_prescription_inventory_item
    ON prescription(inventory_item_id)
    WHERE inventory_item_id IS NOT NULL;