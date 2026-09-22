INSERT INTO categories (name, image_path, active)
SELECT 'Groceries', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Groceries');

INSERT INTO categories (name, image_path, active)
SELECT 'Household', NULL, TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Household');

INSERT INTO products (name, sku, barcode, category_id, base_price, unit, image_path, active)
SELECT 'Premium Rice 5kg', 'RICE-5KG', '893000000001', c.id, 12.90, 'bag', NULL, TRUE
FROM categories c
WHERE c.name = 'Groceries'
  AND NOT EXISTS (SELECT 1 FROM products WHERE sku = 'RICE-5KG')
LIMIT 1;

INSERT INTO products (name, sku, barcode, category_id, base_price, unit, image_path, active)
SELECT 'Laundry Detergent', 'HOME-DETERGENT', '893000000002', c.id, 8.50, 'bottle', NULL, TRUE
FROM categories c
WHERE c.name = 'Household'
  AND NOT EXISTS (SELECT 1 FROM products WHERE sku = 'HOME-DETERGENT')
LIMIT 1;

INSERT INTO customers (email, password_hash, full_name, phone, active)
VALUES ('customer@demo.com', '$2y$10$jK/aVYgYI6uj6LEXUJx5C.ASfjrjAdAiwnHjeGVhNbCqYKL67zqV2', 'Demo Customer', '0900000000', TRUE)
ON DUPLICATE KEY UPDATE
    password_hash = VALUES(password_hash),
    full_name = VALUES(full_name),
    phone = VALUES(phone),
    active = VALUES(active);

INSERT INTO carts (customer_id)
SELECT c.id
FROM customers c
WHERE c.email = 'customer@demo.com'
  AND NOT EXISTS (SELECT 1 FROM carts existing WHERE existing.customer_id = c.id);

INSERT INTO cart_items (cart_id, product_id, quantity)
SELECT cart.id, p.id, 1
FROM carts cart
JOIN customers c ON c.id = cart.customer_id
JOIN products p ON p.sku = 'RICE-5KG'
WHERE c.email = 'customer@demo.com'
  AND NOT EXISTS (
      SELECT 1 FROM cart_items existing
      WHERE existing.cart_id = cart.id AND existing.product_id = p.id
  );

INSERT INTO orders (customer_id, status, total_amount, recipient_name, phone, shipping_address, payment_method)
SELECT c.id, 'PENDING', 8.50, c.full_name, c.phone, '123 Sample Street', 'Cash on delivery'
FROM customers c
WHERE c.email = 'customer@demo.com'
  AND NOT EXISTS (SELECT 1 FROM orders existing WHERE existing.customer_id = c.id);

INSERT INTO order_items (order_id, product_id, product_name, quantity, unit_price, subtotal)
SELECT o.id, p.id, p.name, 1, p.base_price, p.base_price
FROM orders o
JOIN customers c ON c.id = o.customer_id
JOIN products p ON p.sku = 'HOME-DETERGENT'
WHERE c.email = 'customer@demo.com'
  AND NOT EXISTS (SELECT 1 FROM order_items existing WHERE existing.order_id = o.id);
