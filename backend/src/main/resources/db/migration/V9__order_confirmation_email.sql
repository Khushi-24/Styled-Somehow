ALTER TABLE pending_orders ADD COLUMN confirmation_sent_at TIMESTAMP NULL;
ALTER TABLE pending_orders ADD COLUMN confirmation_next_attempt_at TIMESTAMP NULL;
ALTER TABLE pending_orders ADD COLUMN confirmation_attempts INT NOT NULL DEFAULT 0;
-- Do not email historic orders when the feature is enabled.
UPDATE pending_orders SET confirmation_sent_at = CURRENT_TIMESTAMP WHERE status <> 'PENDING_PAYMENT';
CREATE INDEX idx_order_confirmation_queue ON pending_orders (status, confirmation_sent_at, confirmation_next_attempt_at);
