CREATE TABLE shipment_bookings (
 order_id VARCHAR(36) PRIMARY KEY,
 status VARCHAR(30) NOT NULL,
 courier_id INT NOT NULL,
 courier VARCHAR(150) NOT NULL,
 quoted_cost INT NOT NULL,
 provider_order_id BIGINT,
 provider_shipment_id BIGINT,
 awb VARCHAR(100),
 message VARCHAR(500),
 created_at TIMESTAMP(6) NOT NULL,
 updated_at TIMESTAMP(6) NOT NULL,
 FOREIGN KEY (order_id) REFERENCES pending_orders(id),
 UNIQUE (provider_shipment_id),
 UNIQUE (awb)
);
