-- Correct the catalogue without changing the checksum of the applied V7 migration.
UPDATE products
SET gsm = CASE WHEN gsm = 260 THEN 240 ELSE gsm END,
    details = REPLACE(REPLACE(REPLACE(details, '260 GSM', '240 GSM'), '260 gsm', '240 gsm'), '260 Gsm', '240 Gsm'),
    description = REPLACE(REPLACE(REPLACE(description, '260 GSM', '240 GSM'), '260 gsm', '240 gsm'), '260 Gsm', '240 Gsm'),
    version = version + 1
WHERE gsm = 260
   OR LOWER(details) LIKE '%260 gsm%'
   OR LOWER(description) LIKE '%260 gsm%';
