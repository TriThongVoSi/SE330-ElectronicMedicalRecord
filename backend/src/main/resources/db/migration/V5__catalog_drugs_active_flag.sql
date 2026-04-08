ALTER TABLE drugs
    ADD COLUMN is_active BOOLEAN NOT NULL DEFAULT TRUE
    AFTER stock_quantity;

CREATE INDEX idx_drugs_is_active
    ON drugs (is_active);
