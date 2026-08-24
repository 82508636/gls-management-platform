ALTER TABLE customers DROP CONSTRAINT IF EXISTS uq_customers_customer_code;

WITH numbered AS (
    SELECT id, agency, ROW_NUMBER() OVER (PARTITION BY agency ORDER BY created_at, id) AS position
    FROM customers
)
UPDATE customers c
SET customer_code = CASE numbered.agency
    WHEN 'LTFT01' THEN '1' || LPAD(numbered.position::text, 5, '0')
    WHEN 'LTFT02' THEN '2' || LPAD(numbered.position::text, 5, '0')
END
FROM numbered
WHERE c.id = numbered.id;

CREATE TABLE customer_code_counters (
    agency VARCHAR(10) PRIMARY KEY,
    next_number INTEGER NOT NULL CHECK (next_number BETWEEN 1 AND 100000)
);

INSERT INTO customer_code_counters (agency, next_number)
VALUES
    ('LTFT01', COALESCE((SELECT MAX(SUBSTRING(customer_code FROM 2)::INTEGER) + 1 FROM customers WHERE agency = 'LTFT01'), 1)),
    ('LTFT02', COALESCE((SELECT MAX(SUBSTRING(customer_code FROM 2)::INTEGER) + 1 FROM customers WHERE agency = 'LTFT02'), 1));

ALTER TABLE customers ADD CONSTRAINT uq_customers_customer_code UNIQUE (customer_code);
ALTER TABLE customers ADD CONSTRAINT chk_customers_code_agency CHECK (
    (agency = 'LTFT01' AND customer_code LIKE '1_____') OR
    (agency = 'LTFT02' AND customer_code LIKE '2_____')
);
