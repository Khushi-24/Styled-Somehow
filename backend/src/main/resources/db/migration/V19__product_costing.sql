CREATE TABLE costing_profiles (
 product_id BIGINT PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 customer_shipping DECIMAL(12,2) NOT NULL DEFAULT 0,
 allocate_monthly BOOLEAN NOT NULL DEFAULT FALSE,
 updated_at TIMESTAMP(6),
 FOREIGN KEY (product_id) REFERENCES products(id)
);
CREATE TABLE costing_items (
 product_id BIGINT NOT NULL,
 position INT NOT NULL,
 cost_key VARCHAR(30) NOT NULL,
 amount DECIMAL(12,2),
 included BOOLEAN NOT NULL,
 PRIMARY KEY(product_id,position),
 UNIQUE(product_id,cost_key),
 FOREIGN KEY(product_id) REFERENCES costing_profiles(product_id)
);
CREATE TABLE costing_months (
 month_key VARCHAR(7) PRIMARY KEY,
 version BIGINT NOT NULL DEFAULT 0,
 electricity DECIMAL(12,2) NOT NULL DEFAULT 0,
 website DECIMAL(12,2) NOT NULL DEFAULT 0,
 ai DECIMAL(12,2) NOT NULL DEFAULT 0,
 other DECIMAL(12,2) NOT NULL DEFAULT 0,
 expected_units INT NOT NULL DEFAULT 0
);
