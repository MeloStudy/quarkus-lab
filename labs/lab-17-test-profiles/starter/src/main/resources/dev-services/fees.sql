-- Loaded by Dev Services into the throwaway database (dev mode and tests). Production never sees this file run:
-- its schema belongs to migrations (lab 11). The table is not an entity: Hibernate does not know it exists.
CREATE TABLE exchange_fee (
    from_code CHAR(3)      NOT NULL,
    to_code   CHAR(3)      NOT NULL,
    percent   NUMERIC(4,2) NOT NULL,
    PRIMARY KEY (from_code, to_code)
);
INSERT INTO exchange_fee (from_code, to_code, percent) VALUES
    ('USD', 'EUR', 1.00),
    ('EUR', 'USD', 1.50),
    ('USD', 'GBP', 0.75);
