-- Durable once-only refund guard. A stuck PROCESSING or REVIEW_REQUIRED attempt
-- must be reconciled with the gateway before manual recovery. NEVER auto-retry.
CREATE TABLE refund_attempts (
    id BIGSERIAL PRIMARY KEY,
    order_id INTEGER NOT NULL UNIQUE REFERENCES orders(id),
    payment_id INTEGER NOT NULL REFERENCES payments(id),
    request_id VARCHAR(100) NOT NULL UNIQUE,
    amount NUMERIC(15, 2) NOT NULL CHECK (amount > 0),
    status VARCHAR(30) NOT NULL CHECK (status IN ('PROCESSING', 'SUCCEEDED', 'REVIEW_REQUIRED')),
    reason TEXT,
    requested_by_user_id INTEGER NOT NULL REFERENCES users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_refund_attempts_status ON refund_attempts(status);
