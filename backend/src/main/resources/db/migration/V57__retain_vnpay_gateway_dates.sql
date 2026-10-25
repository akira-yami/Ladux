-- Exact gateway dates are required by querydr and refund.
ALTER TABLE payments ADD COLUMN vnp_create_date VARCHAR(14);
ALTER TABLE payments ADD COLUMN vnp_pay_date VARCHAR(14);
ALTER TABLE payments ADD COLUMN vnp_queried_at TIMESTAMPTZ;
ALTER TABLE payments ADD COLUMN requires_review BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE payments SET vnp_create_date = substring(payment_url FROM 'vnp_CreateDate=([0-9]{14})')
WHERE provider = 'VNPAY' AND payment_url IS NOT NULL;
