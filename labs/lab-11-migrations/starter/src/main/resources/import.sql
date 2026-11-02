-- Sample data for dev mode (Hibernate runs import.sql after creating the schema in dev and test).
INSERT INTO product (name, name_key, category, price, stock) VALUES ('Clean Code', 'clean code', 'books', 30.50, 10);
INSERT INTO product (name, name_key, category, price, stock) VALUES ('Effective Java', 'effective java', 'books', 45.00, 10);
INSERT INTO product (name, name_key, category, price, stock) VALUES ('Domain-Driven Design', 'domain-driven design', 'books', 50.25, 10);
INSERT INTO product (name, name_key, category, price, stock) VALUES ('Chess', 'chess', 'games', 20.00, 10);
INSERT INTO product (name, name_key, category, price, stock) VALUES ('Catan', 'catan', 'games', 40.50, 10);
