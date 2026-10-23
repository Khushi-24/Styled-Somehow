CREATE TABLE commerce_control (id BIGINT PRIMARY KEY, orders_paused BOOLEAN NOT NULL, updated_at TIMESTAMP(6), updated_by VARCHAR(100));
INSERT INTO commerce_control(id,orders_paused) VALUES (1,FALSE);
CREATE TABLE commerce_attempts (id VARCHAR(36) PRIMARY KEY, scope VARCHAR(20) NOT NULL, ip_hash VARCHAR(64) NOT NULL, email_hash VARCHAR(64) NOT NULL, browser_hash VARCHAR(64) NOT NULL, created_at TIMESTAMP(6) NOT NULL);
CREATE INDEX commerce_attempts_window ON commerce_attempts(scope,created_at);
