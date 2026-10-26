CREATE TABLE test_order_deletions (
 order_id VARCHAR(36) PRIMARY KEY,
 previous_status VARCHAR(30) NOT NULL,
 actor VARCHAR(100) NOT NULL,
 reason VARCHAR(400) NOT NULL,
 deleted_at TIMESTAMP(6) NOT NULL,
 stock_restored BOOLEAN NOT NULL,
 restored_units INT NOT NULL,
 released_units INT NOT NULL
);
CREATE INDEX order_access_order_cleanup ON order_access_challenges(order_id);
