CREATE TABLE shop_collections (
 slug VARCHAR(100) PRIMARY KEY,
 name VARCHAR(100) NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE,
 sort_order INT NOT NULL DEFAULT 0
);
INSERT INTO shop_collections (slug,name,sort_order) VALUES ('women','Women',0),('men','Men',1);
