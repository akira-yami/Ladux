-- V23: historically this migration seeded demo suppliers and product associations.
-- Keep the version resolved on databases that already applied it, but never assume
-- production has products 1..12 (V3 demo products live in db/devdata only).
--
-- IMPORTANT: Because this corrects an already-versioned migration, databases
-- that previously applied V23 must have their checksums reconciled by a DBA
-- after verifying this exact change. Never enable automatic Flyway repair on prod.
-- A fresh production database has no demo product and inserts nothing.

INSERT INTO suppliers (id, name, address, phone, email, is_active)
SELECT s.id, s.name, s.address, s.phone, s.email, TRUE
FROM (VALUES
    (1, 'Cong ty TNHH Phan Phoi Cong Nghe FPT', '261 Khanh Hoi, Quan 4, TP. Ho Chi Minh', '02873002222', 'sales@fpt-dist.vn'),
    (2, 'Cong ty CP The Gioi So Digiworld', '102 Nguyen Dinh Chinh, Phu Nhuan, TP. Ho Chi Minh', '02839971234', 'b2b@digiworld.vn'),
    (3, 'Cong ty TNHH Synnex Viet Nam', '459 Su Van Hanh, Quan 10, TP. Ho Chi Minh', '02838334455', 'order@synnex.vn')
) AS s(id, name, address, phone, email)
WHERE EXISTS (SELECT 1 FROM products WHERE id = 1 AND slug = 'laptop-apple-macbook-air-m3-13')
ON CONFLICT (id) DO NOTHING;

-- Only seed associations when the demo catalog and demo suppliers are present.
INSERT INTO product_suppliers (product_id, supplier_id, cost_price, lead_time_days)
SELECT p.product_id, p.supplier_id, p.cost_price, p.lead_time_days
FROM (VALUES
    (1, 1, 2300.00, 7), (1, 2, 2350.00, 5), (2, 1, 1950.00, 10),
    (3, 3, 2900.00, 14), (4, 2, 1500.00, 7), (5, 1, 1750.00, 7),
    (6, 2, 1280.00, 5), (7, 3, 2650.00, 12), (8, 3, 3950.00, 21),
    (9, 1, 1850.00, 7), (10, 2, 540.00, 5), (11, 1, 2050.00, 9),
    (12, 2, 280.00, 3)
) AS p(product_id, supplier_id, cost_price, lead_time_days)
WHERE EXISTS (SELECT 1 FROM products x WHERE x.id = p.product_id)
  AND EXISTS (SELECT 1 FROM suppliers s WHERE s.id = p.supplier_id)
ON CONFLICT DO NOTHING;

-- A new DB without seed still needs valid SERIAL sequences (avoid setval(..., NULL)).
SELECT setval(pg_get_serial_sequence('suppliers', 'id'),
              GREATEST(COALESCE((SELECT MAX(id) FROM suppliers), 0), 1),
              (SELECT COUNT(*) > 0 FROM suppliers));
