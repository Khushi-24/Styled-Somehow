-- One shared oversized blank garment type, confirmed by Khushi. Initial quantities applied once.
CREATE TABLE blank_stock (id BIGINT AUTO_INCREMENT PRIMARY KEY, version BIGINT NOT NULL DEFAULT 0, colour VARCHAR(255), size VARCHAR(255), quantity INT NOT NULL, low_stock_threshold INT NOT NULL, UNIQUE(colour,size), CHECK(quantity>=0));
CREATE TABLE stock_movements (id BIGINT AUTO_INCREMENT PRIMARY KEY, request_id VARCHAR(255) NOT NULL UNIQUE, stock_id BIGINT, colour VARCHAR(255), size VARCHAR(255), delta INT NOT NULL, resulting_quantity INT NOT NULL, reason VARCHAR(500), actor VARCHAR(255), occurred_at TIMESTAMP(6), FOREIGN KEY(stock_id) REFERENCES blank_stock(id));
INSERT INTO blank_stock (id,colour,size,quantity,low_stock_threshold) VALUES (1,'White','S',12,3);
INSERT INTO stock_movements (request_id,stock_id,colour,size,delta,resulting_quantity,reason,actor,occurred_at) VALUES ('opening-stock-1',1,'White','S',12,12,'Opening stock confirmed by owner: 12 each S/M/L; XL zero','opening-import',CURRENT_TIMESTAMP);
INSERT INTO blank_stock (id,colour,size,quantity,low_stock_threshold) VALUES (2,'White','M',12,3);
INSERT INTO stock_movements (request_id,stock_id,colour,size,delta,resulting_quantity,reason,actor,occurred_at) VALUES ('opening-stock-2',2,'White','M',12,12,'Opening stock confirmed by owner: 12 each S/M/L; XL zero','opening-import',CURRENT_TIMESTAMP);
INSERT INTO blank_stock (id,colour,size,quantity,low_stock_threshold) VALUES (3,'White','L',12,3);
INSERT INTO stock_movements (request_id,stock_id,colour,size,delta,resulting_quantity,reason,actor,occurred_at) VALUES ('opening-stock-3',3,'White','L',12,12,'Opening stock confirmed by owner: 12 each S/M/L; XL zero','opening-import',CURRENT_TIMESTAMP);
INSERT INTO blank_stock (id,colour,size,quantity,low_stock_threshold) VALUES (4,'White','XL',0,3);
INSERT INTO stock_movements (request_id,stock_id,colour,size,delta,resulting_quantity,reason,actor,occurred_at) VALUES ('opening-stock-4',4,'White','XL',0,0,'Opening stock confirmed by owner: 12 each S/M/L; XL zero','opening-import',CURRENT_TIMESTAMP);
INSERT INTO blank_stock (id,colour,size,quantity,low_stock_threshold) VALUES (5,'Black','S',12,3);
INSERT INTO stock_movements (request_id,stock_id,colour,size,delta,resulting_quantity,reason,actor,occurred_at) VALUES ('opening-stock-5',5,'Black','S',12,12,'Opening stock confirmed by owner: 12 each S/M/L; XL zero','opening-import',CURRENT_TIMESTAMP);
INSERT INTO blank_stock (id,colour,size,quantity,low_stock_threshold) VALUES (6,'Black','M',12,3);
INSERT INTO stock_movements (request_id,stock_id,colour,size,delta,resulting_quantity,reason,actor,occurred_at) VALUES ('opening-stock-6',6,'Black','M',12,12,'Opening stock confirmed by owner: 12 each S/M/L; XL zero','opening-import',CURRENT_TIMESTAMP);
INSERT INTO blank_stock (id,colour,size,quantity,low_stock_threshold) VALUES (7,'Black','L',12,3);
INSERT INTO stock_movements (request_id,stock_id,colour,size,delta,resulting_quantity,reason,actor,occurred_at) VALUES ('opening-stock-7',7,'Black','L',12,12,'Opening stock confirmed by owner: 12 each S/M/L; XL zero','opening-import',CURRENT_TIMESTAMP);
INSERT INTO blank_stock (id,colour,size,quantity,low_stock_threshold) VALUES (8,'Black','XL',0,3);
INSERT INTO stock_movements (request_id,stock_id,colour,size,delta,resulting_quantity,reason,actor,occurred_at) VALUES ('opening-stock-8',8,'Black','XL',0,0,'Opening stock confirmed by owner: 12 each S/M/L; XL zero','opening-import',CURRENT_TIMESTAMP);
