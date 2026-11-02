-- Sample data for dev mode only (db/dev is a Flyway location of the dev profile).
-- Repeatable migration: it runs again whenever the file changes, so it must be idempotent.
INSERT INTO product (name, name_key, category, price, stock) VALUES
    ('Clean Code', 'clean code', 'books', 30.50, 10),
    ('Effective Java', 'effective java', 'books', 45.00, 10),
    ('Domain-Driven Design', 'domain-driven design', 'books', 50.25, 10),
    ('Chess', 'chess', 'games', 20.00, 10),
    ('Catan', 'catan', 'games', 40.50, 10)
ON CONFLICT (name_key) DO NOTHING;
