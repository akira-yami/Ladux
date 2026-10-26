-- V62: production hardening (an toan tren du lieu hien co).
--
-- 1) Coupon: gioi han so lan dung theo tung khach (nullable -> giu nguyen hanh vi cu).
-- 2) CHECK constraint cho tien/so luong. Dung NOT VALID: rang buoc duoc ap dung NGAY cho moi
--    INSERT/UPDATE moi, nhung khong quet/kiem tra lai du lieu lich su (khong khoa bang lau, khong
--    lam hong migration neu du lieu cu co dong bat thuong). Sau khi doi soat du lieu cu co the chay:
--      ALTER TABLE <table> VALIDATE CONSTRAINT <name>;   (chi giu SHARE UPDATE EXCLUSIVE)
-- 3) Index cho cac truy van nong chua duoc phuc vu; bo index trung lap.

ALTER TABLE coupons ADD COLUMN IF NOT EXISTS per_user_limit INTEGER;

-- ---------------------------------------------------------------------------
-- 2) Integrity checks
-- ---------------------------------------------------------------------------
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_pv_price_non_negative') THEN
        ALTER TABLE product_variants ADD CONSTRAINT ck_pv_price_non_negative
            CHECK (price >= 0) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_pv_discount_price_range') THEN
        ALTER TABLE product_variants ADD CONSTRAINT ck_pv_discount_price_range
            CHECK (discount_price IS NULL OR (discount_price >= 0 AND discount_price <= price)) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_cart_items_quantity_positive') THEN
        ALTER TABLE cart_items ADD CONSTRAINT ck_cart_items_quantity_positive
            CHECK (quantity > 0) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_order_items_quantity_positive') THEN
        ALTER TABLE order_items ADD CONSTRAINT ck_order_items_quantity_positive
            CHECK (quantity > 0) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_order_items_price_non_negative') THEN
        ALTER TABLE order_items ADD CONSTRAINT ck_order_items_price_non_negative
            CHECK (price_at_purchase >= 0) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_orders_amounts_non_negative') THEN
        ALTER TABLE orders ADD CONSTRAINT ck_orders_amounts_non_negative
            CHECK (sub_total >= 0 AND discount_amount >= 0 AND final_amount >= 0 AND shipping_fee >= 0) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_orders_discount_not_above_subtotal') THEN
        ALTER TABLE orders ADD CONSTRAINT ck_orders_discount_not_above_subtotal
            CHECK (discount_amount <= sub_total) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_payments_amount_non_negative') THEN
        ALTER TABLE payments ADD CONSTRAINT ck_payments_amount_non_negative
            CHECK (amount >= 0) NOT VALID;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'ck_coupons_values') THEN
        ALTER TABLE coupons ADD CONSTRAINT ck_coupons_values
            CHECK (discount_value > 0
                   AND min_order_value >= 0
                   AND used_count >= 0
                   AND (usage_limit IS NULL OR usage_limit > 0)
                   AND (per_user_limit IS NULL OR per_user_limit > 0)
                   AND (max_discount_amount IS NULL OR max_discount_amount > 0)) NOT VALID;
    END IF;
END $$;

-- ---------------------------------------------------------------------------
-- 3) Indexes for real query patterns
-- ---------------------------------------------------------------------------

-- OrderItemRepository.existsByProductVariantId (chan xoa variant da ban) va join theo SKU.
-- Truoc day khong co index -> seq scan toan bang order_items.
CREATE INDEX IF NOT EXISTS idx_order_items_variant ON order_items (product_variant_id);

-- Best-seller 30 ngay (ProductDiscoveryRepository): loc DELIVERED theo thoi gian truoc khi gom nhom.
CREATE INDEX IF NOT EXISTS idx_order_histories_delivered_at
    ON order_histories (created_at) INCLUDE (order_id) WHERE status = 'DELIVERED';

-- Hop thu thong bao cua user: user_id = ? AND is_deleted_by_user = false [AND is_read = ?] ORDER BY created_at DESC.
CREATE INDEX IF NOT EXISTS idx_notifications_inbox
    ON notifications (user_id, created_at DESC) WHERE is_deleted_by_user = false;
CREATE INDEX IF NOT EXISTS idx_notifications_unread
    ON notifications (user_id) WHERE is_read = false AND is_deleted_by_user = false;
-- Danh sach thong bao phia admin sap xep theo thoi gian.
CREATE INDEX IF NOT EXISTS idx_notifications_created_at ON notifications (created_at DESC);

-- So cai kho: loc theo SKU va sap xep moi nhat truoc.
CREATE INDEX IF NOT EXISTS idx_stock_movements_variant_created
    ON stock_movements (product_variant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_stock_movements_created_at ON stock_movements (created_at DESC);

-- Job doi soat VNPay va hang doi "can doi soat" cua admin.
CREATE INDEX IF NOT EXISTS idx_payments_vnpay_pending
    ON payments (created_at) WHERE provider = 'VNPAY' AND status = 'PENDING' AND payment_url IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_payments_requires_review
    ON payments (created_at DESC) WHERE requires_review = true;

-- Gioi han so don PENDING moi khach (OrderServiceImpl.createOrder) da duoc phuc vu boi
-- idx_orders_user_created_at? Khong: can loc theo status -> index rieng nho gon.
CREATE INDEX IF NOT EXISTS idx_orders_user_pending ON orders (user_id) WHERE status = 'PENDING';

-- Bo index trung lap (V29 tao lai index tren cung cot product_id voi ten gay hieu nham).
DROP INDEX IF EXISTS idx_cart_items_product_variant_id;   -- trung idx_cart_items_product_id
DROP INDEX IF EXISTS idx_order_items_product_variant_id;  -- trung idx_order_items_product_id (cot product_id)
-- Duoc thay the boi idx_stock_movements_variant_created (cung tien to product_variant_id).
DROP INDEX IF EXISTS idx_stock_movements_product_variant;
