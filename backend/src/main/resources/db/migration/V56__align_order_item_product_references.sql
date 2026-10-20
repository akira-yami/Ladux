-- V29 made order_items.product_id refer to a SKU. V30 added a separate SKU column,
-- but did not backfill it or restore the product foreign key used by OrderItem.
-- Preserve each historical SKU, price and quantity; derive its parent from catalog.
DO $$
DECLARE
    reference RECORD;
BEGIN
    FOR reference IN
        SELECT DISTINCT tc.constraint_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name AND tc.table_schema = kcu.table_schema
        WHERE tc.table_schema = 'public' AND tc.table_name = 'order_items'
          AND tc.constraint_type = 'FOREIGN KEY' AND kcu.column_name = 'product_id'
    LOOP
        EXECUTE format('ALTER TABLE order_items DROP CONSTRAINT %I', reference.constraint_name);
    END LOOP;
END $$;

UPDATE order_items oi
SET product_variant_id = pv.id, product_id = pv.product_id
FROM product_variants pv
WHERE oi.product_variant_id IS NULL AND oi.product_id = pv.id;

UPDATE order_items oi
SET product_id = pv.product_id
FROM product_variants pv
WHERE oi.product_variant_id = pv.id AND oi.product_id IS DISTINCT FROM pv.product_id;

-- Fail transactionally on unidentified legacy lines instead of silently deleting history.
ALTER TABLE order_items ALTER COLUMN product_variant_id SET NOT NULL;
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_product
    FOREIGN KEY (product_id) REFERENCES products(id);

CREATE INDEX IF NOT EXISTS idx_order_histories_delivered
    ON order_histories (order_id, created_at) WHERE status = 'DELIVERED';
CREATE INDEX IF NOT EXISTS idx_product_variants_sellable
    ON product_variants (product_id) WHERE is_active = true AND stock_quantity > 0;
