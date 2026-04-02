ALTER TABLE service ADD COLUMN sku VARCHAR(20);
ALTER TABLE service ADD COLUMN unit VARCHAR(10);

CREATE UNIQUE INDEX idx_service_sku_unique
ON service (clinic_id, sku)
WHERE sku IS NOT NULL AND deleted = false;
