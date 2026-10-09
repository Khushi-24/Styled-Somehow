ALTER TABLE pending_orders ADD gateway_order_id VARCHAR(100);
ALTER TABLE pending_orders ADD payment_id VARCHAR(100);
ALTER TABLE pending_orders ADD payment_mode VARCHAR(255);
ALTER TABLE pending_orders ADD paid_at TIMESTAMP(6);
CREATE UNIQUE INDEX order_gateway_reference ON pending_orders(gateway_order_id);
CREATE TABLE payment_receipts (id VARCHAR(100) PRIMARY KEY, order_id VARCHAR(36), amount BIGINT NOT NULL, status VARCHAR(255), refund_id VARCHAR(100), refund_key VARCHAR(36), reason VARCHAR(500), retries INT NOT NULL, created_at TIMESTAMP(6), updated_at TIMESTAMP(6), FOREIGN KEY(order_id) REFERENCES pending_orders(id));
CREATE INDEX refund_work_queue ON payment_receipts(status,updated_at);

ALTER TABLE pending_orders ADD COLUMN payment_checked_at TIMESTAMP(6) NULL;
