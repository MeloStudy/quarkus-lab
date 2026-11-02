-- V4: the shop starts tracking what it pays for each product (internal data, never shown by the API).
-- Nullable on purpose: existing rows have no cost yet, and adding NOT NULL to a populated table needs a backfill first.
ALTER TABLE product ADD COLUMN cost_price NUMERIC(9, 2);
ALTER TABLE product ADD CONSTRAINT product_cost_not_negative CHECK (cost_price IS NULL OR cost_price >= 0);
