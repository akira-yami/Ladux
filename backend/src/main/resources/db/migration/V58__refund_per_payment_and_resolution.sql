-- An order may receive a second payment after an earlier attempt was retried.
-- Each payment can be refunded once; existing guards and audit rows are retained.
ALTER TABLE refund_attempts DROP CONSTRAINT refund_attempts_order_id_key;
ALTER TABLE refund_attempts ADD CONSTRAINT uq_refund_attempt_payment UNIQUE (payment_id);
CREATE INDEX idx_refund_attempt_order ON refund_attempts(order_id);
ALTER TABLE refund_attempts ADD COLUMN resolution_reference VARCHAR(1000);
ALTER TABLE refund_attempts ADD COLUMN resolved_by_user_id INTEGER REFERENCES users(id);
