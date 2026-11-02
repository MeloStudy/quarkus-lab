-- V2: rules Hibernate cannot declare. The application validates them too, but only the database
-- protects the data from every other writer (scripts, other services, a bug in this one).

-- Case-insensitive name uniqueness enforced on the real column, not on the derived name_key
CREATE UNIQUE INDEX product_name_lower_uk ON product (lower(name));

ALTER TABLE product ADD CONSTRAINT product_price_positive CHECK (price > 0);
ALTER TABLE product ADD CONSTRAINT product_stock_not_negative CHECK (stock >= 0);
ALTER TABLE review ADD CONSTRAINT review_rating_range CHECK (rating BETWEEN 1 AND 5);
