ALTER TABLE pending_orders ADD COLUMN fulfilment_stage VARCHAR(30);
UPDATE pending_orders SET fulfilment_stage = CASE WHEN status = 'PAID' THEN 'PAID' ELSE 'UNFULFILLED' END;
UPDATE pending_orders SET fulfilment_stage = 'COURIER_BOOKED' WHERE id IN (SELECT order_id FROM shipment_bookings WHERE status = 'BOOKED') AND status = 'PAID';
CREATE TABLE fulfilment_events (
 id VARCHAR(36) PRIMARY KEY,
 order_id VARCHAR(36) NOT NULL,
 stage VARCHAR(30) NOT NULL,
 actor VARCHAR(100) NOT NULL,
 note VARCHAR(500) NOT NULL,
 occurred_at TIMESTAMP NOT NULL,
 FOREIGN KEY (order_id) REFERENCES pending_orders(id) ON DELETE CASCADE
);
CREATE INDEX idx_fulfilment_events_order ON fulfilment_events(order_id, occurred_at);
ALTER TABLE shipment_bookings ADD COLUMN courier_status VARCHAR(150);
ALTER TABLE shipment_bookings ADD COLUMN tracking_url VARCHAR(1000);
ALTER TABLE shipment_bookings ADD COLUMN tracking_checked_at TIMESTAMP NULL;
ALTER TABLE shipment_bookings ADD COLUMN tracking_failed BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE shipment_bookings ADD COLUMN tracking_terminal BOOLEAN NOT NULL DEFAULT FALSE;
CREATE INDEX idx_shipment_tracking_due ON shipment_bookings(status, tracking_terminal, tracking_checked_at);
