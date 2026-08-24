ALTER TABLE customers RENAME COLUMN name TO shipping_name;
ALTER TABLE customers RENAME COLUMN email TO contact_email;
ALTER TABLE customers RENAME COLUMN gls_account TO account_code;
ALTER TABLE customers RENAME COLUMN gls_customer_reference TO billing_reference;

ALTER TABLE customers
    ADD COLUMN customer_code VARCHAR(6),
    ADD COLUMN abbreviation VARCHAR(30),
    ADD COLUMN agency VARCHAR(10),
    ADD COLUMN address VARCHAR(500),
    ADD COLUMN postal_code VARCHAR(20),
    ADD COLUMN locality VARCHAR(120),
    ADD COLUMN country VARCHAR(2),
    ADD COLUMN mobile VARCHAR(50),
    ADD COLUMN billing_country VARCHAR(2),
    ADD COLUMN billing_legal_name VARCHAR(200),
    ADD COLUMN billing_postal_code VARCHAR(20),
    ADD COLUMN billing_locality VARCHAR(120),
    ADD COLUMN customer_type VARCHAR(20),
    ADD COLUMN responsible_name VARCHAR(200),
    ADD COLUMN billing_email VARCHAR(254),
    ADD COLUMN default_document VARCHAR(50),
    ADD COLUMN exchange_rate NUMERIC(18,6),
    ADD COLUMN currency VARCHAR(3),
    ADD COLUMN invoice_by_post BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN documents_by_email BOOLEAN NOT NULL DEFAULT FALSE;

WITH numbered AS (SELECT id, ROW_NUMBER() OVER (ORDER BY created_at) AS position FROM customers)
UPDATE customers c SET customer_code = LPAD(numbered.position::text, 6, '0') FROM numbered WHERE c.id = numbered.id;
UPDATE customers SET agency = 'LTFT01', country = 'PT', billing_country = 'PT',
    billing_legal_name = shipping_name, customer_type = 'COMPANY', default_document = 'INVOICE', currency = 'EUR';

ALTER TABLE customers ALTER COLUMN customer_code SET NOT NULL;
ALTER TABLE customers ALTER COLUMN agency SET NOT NULL;
ALTER TABLE customers ALTER COLUMN customer_type SET NOT NULL;
ALTER TABLE customers ADD CONSTRAINT uq_customers_customer_code UNIQUE (customer_code);
ALTER TABLE customers ADD CONSTRAINT chk_customers_customer_code CHECK (customer_code ~ '^[0-9]{6}$');
ALTER TABLE customers ADD CONSTRAINT chk_customers_agency CHECK (agency IN ('LTFT01', 'LTFT02'));
