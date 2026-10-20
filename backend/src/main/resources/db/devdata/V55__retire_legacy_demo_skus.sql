-- Development/test data repair only. V28 copied the V3 legacy demo prices into
-- LAP-0001..LAP-0012; V38 added VND alternatives without replacing those SKUs.
-- Retire only unchanged, positively identified demo rows. Preserve their prices,
-- stock and every historical order reference; never infer a currency conversion.
WITH demo(id, slug, legacy_price, legacy_discount, alternative_sku, alternative_price, alternative_discount) AS (
    VALUES
        (1, 'laptop-apple-macbook-air-m3-13', 2899.00, 2699.00, 'MAC-AIR-M3-16-512-BLK', 33990000.00, 32490000.00),
        (2, 'laptop-gaming-lenovo-legion-5-pro-16', 2499.00, 2299.00, 'LEGION-5PRO-32-1TB-GRY', 42990000.00, 40990000.00),
        (3, 'laptop-do-hoa-dell-precision-5680', 3599.00, NULL, 'PRECISION-5680-64-2TB-GRY', 95000000.00, 89990000.00),
        (4, 'laptop-van-phong-hp-probook-450-g10', 1899.00, 1749.00, 'PROBOOK-450-32-1TB-SLV', 25500000.00, 23990000.00),
        (5, 'laptop-gaming-asus-tuf-gaming-f15', 2199.00, 2099.00, 'ASUS-TUF-F15-32-1TB-BLK', 30990000.00, 28990000.00),
        (6, 'laptop-sinh-vien-acer-aspire-7-a715', 1599.00, NULL, 'ASPIRE-7-16-512-BLK', 19500000.00, 17490000.00),
        (7, 'workstation-msi-creator-z16', 3299.00, 3099.00, 'MSI-Z16-64-2TB-WHT', 64990000.00, 59990000.00),
        (8, 'gaming-cao-cap-razer-blade-16', 4899.00, NULL, 'RAZER-B16-64-2TB-BLK', 109990000.00, 102990000.00),
        (9, 'ultrabook-samsung-galaxy-book-4', 2299.00, 2099.00, 'GALAXY-B4-32-1TB-GRY', 41990000.00, 38990000.00),
        (10, 'msi-thin-15-b13uc', 699.00, 649.00, 'MSI-THIN15-16-512-BLK', 20490000.00, 17990000.00),
        (11, 'laptop-doanh-nhan-microsoft-surface-laptop-6', 2599.00, 2399.00, 'SURFACE-L6-32-1TB-BLK', 47990000.00, 44990000.00),
        (12, 'acer-aspire-5', 399.00, 349.00, 'ASPIRE-5-32-1TB-SLV', 22500000.00, 19990000.00)
)
UPDATE product_variants legacy
SET is_active = false
FROM products p
JOIN demo ON p.id = demo.id AND p.slug = demo.slug
WHERE legacy.product_id = p.id
  AND legacy.is_active = true
  AND legacy.sku = 'LAP-' || lpad(p.id::text, 4, '0')
  AND p.sku = legacy.sku
  AND p.base_price = demo.legacy_price
  AND p.discount_price IS NOT DISTINCT FROM demo.legacy_discount
  AND legacy.price = p.base_price
  AND legacy.discount_price IS NOT DISTINCT FROM p.discount_price
  AND (
      SELECT COUNT(*)
      FROM demo expected_product
      JOIN products confirmed ON confirmed.id = expected_product.id AND confirmed.slug = expected_product.slug
  ) = 12
  AND EXISTS (
      SELECT 1
      FROM product_variants alternative
      WHERE alternative.product_id = p.id
        AND alternative.id <> legacy.id
        AND alternative.is_active = true
        AND alternative.sku = demo.alternative_sku
        AND alternative.price = demo.alternative_price
        AND alternative.discount_price IS NOT DISTINCT FROM demo.alternative_discount
  );
