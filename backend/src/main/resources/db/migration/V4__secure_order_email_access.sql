CREATE TABLE order_access_budget (id BIGINT PRIMARY KEY);
INSERT INTO order_access_budget(id) VALUES(1);
CREATE TABLE order_access_challenges (id VARCHAR(36) PRIMARY KEY, order_id VARCHAR(36), email_hash VARCHAR(64), ip_hash VARCHAR(64), browser_hash VARCHAR(64), code_hash VARCHAR(64), created_at TIMESTAMP(6), expires_at TIMESTAMP(6), attempts INT NOT NULL, consumed BOOLEAN NOT NULL);
CREATE INDEX order_access_email_limit ON order_access_challenges(email_hash,created_at);
CREATE INDEX order_access_ip_limit ON order_access_challenges(ip_hash,created_at);
CREATE INDEX order_access_browser_limit ON order_access_challenges(browser_hash,created_at);
CREATE INDEX order_access_daily_limit ON order_access_challenges(created_at);
