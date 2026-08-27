ALTER TABLE customers ADD COLUMN vat_key VARCHAR(60);

WITH normalized AS (
    SELECT id,
           CASE
               WHEN UPPER(COALESCE(NULLIF(billing_country, ''), NULLIF(country, ''), 'XX')) = 'GR' THEN 'EL'
               ELSE UPPER(COALESCE(NULLIF(billing_country, ''), NULLIF(country, ''), 'XX'))
           END AS country_code,
           REGEXP_REPLACE(UPPER(vat_number), '[-[:space:]./]', '', 'g') AS compact_vat
    FROM customers
), canonical AS (
    SELECT id, country_code,
           CASE
               WHEN compact_vat LIKE country_code || '%' THEN SUBSTRING(compact_vat FROM LENGTH(country_code) + 1)
               WHEN country_code = 'EL' AND compact_vat LIKE 'GR%' THEN SUBSTRING(compact_vat FROM 3)
               ELSE compact_vat
           END AS local_vat
    FROM normalized
)
UPDATE customers customer
SET vat_number = canonical.local_vat,
    vat_key = canonical.country_code || ':' || canonical.local_vat
FROM canonical
WHERE customer.id = canonical.id;

ALTER TABLE customers ALTER COLUMN vat_key SET NOT NULL;
ALTER TABLE customers DROP CONSTRAINT IF EXISTS customers_vat_number_key;
CREATE UNIQUE INDEX uq_customers_vat_key ON customers (vat_key);
