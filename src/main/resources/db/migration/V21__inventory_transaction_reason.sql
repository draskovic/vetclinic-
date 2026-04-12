-- ============================================================
-- V21: reason kolona na inventory_transaction
-- Obavezan razlog za ADJUSTMENT/EXPIRED transakcije (audit trail)
-- ============================================================

ALTER TABLE inventory_transaction
    ADD COLUMN reason VARCHAR(50);

-- Indeks za eventualne buduće izveštaje "Otpis po razlogu"
CREATE INDEX idx_inv_transaction_reason
    ON inventory_transaction(clinic_id, reason)
    WHERE deleted = false AND reason IS NOT NULL;
