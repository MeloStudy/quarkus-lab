-- V3: legacy rows were stored exactly as typed ("Books", " GAMES "). Order matters: fix the data FIRST,
-- then add the constraint; the other way round, the ALTER fails because existing rows violate it.
UPDATE product SET category = lower(trim(category)) WHERE category <> lower(trim(category));

ALTER TABLE product ADD CONSTRAINT product_category_normalized CHECK (category = lower(trim(category)));
