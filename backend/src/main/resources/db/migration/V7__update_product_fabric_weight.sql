-- Update existing catalogue entries and invalidate older admin editor versions.
UPDATE products
SET gsm = 260,
    details = REPLACE(REPLACE(details, '240 GSM', '260 GSM'), '240 gsm', '260 gsm'),
    description = REPLACE(REPLACE(description, '240 GSM', '260 GSM'), '240 gsm', '260 gsm'),
    version = version + 1;
