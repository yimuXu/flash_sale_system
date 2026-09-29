TRUNCATE orders, users, product RESTART IDENTITY CASCADE;

INSERT INTO product (name, price, stock, version) VALUES ('iPhone', 999, :stock, 0);

INSERT INTO users (email, password, role)
SELECT 'user' || i || '@test.com', 'x', 'USER' FROM generate_series(1, 1000) AS i;
