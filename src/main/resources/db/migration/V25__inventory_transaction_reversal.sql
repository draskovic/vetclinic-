-- Storno (reversal) za inventar transakcije
-- reversal_of_transaction_id: na storno zapisu, referenca na originalnu transakciju (self-FK)
-- is_reversed: na originalu, flag da je stornirana (izbegava N+1 exists upit u listi)

ALTER TABLE inventory_transaction
    ADD COLUMN reversal_of_transaction_id UUID NULL,
    ADD COLUMN is_reversed BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE inventory_transaction
    ADD CONSTRAINT fk_inv_tx_reversal
    FOREIGN KEY (reversal_of_transaction_id)
    REFERENCES inventory_transaction(id);

CREATE INDEX idx_inv_tx_reversal_of
    ON inventory_transaction(reversal_of_transaction_id)
    WHERE reversal_of_transaction_id IS NOT NULL;