CREATE TABLE customers (
    id UUID PRIMARY KEY,
    customer_code VARCHAR(6) NOT NULL UNIQUE,
    abbreviation VARCHAR(30),
    shipping_name VARCHAR(200) NOT NULL,
    agency VARCHAR(10) NOT NULL,
    address VARCHAR(500) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    locality VARCHAR(120) NOT NULL,
    country VARCHAR(2) NOT NULL,
    contact_email VARCHAR(254),
    mobile VARCHAR(50),
    phone VARCHAR(50),
    billing_country VARCHAR(2),
    vat_number VARCHAR(50) NOT NULL,
    vat_key VARCHAR(60) NOT NULL UNIQUE,
    billing_legal_name VARCHAR(200),
    billing_address VARCHAR(500),
    billing_postal_code VARCHAR(20),
    billing_locality VARCHAR(120),
    account_code VARCHAR(100),
    billing_reference VARCHAR(100),
    customer_type VARCHAR(20) NOT NULL,
    responsible_name VARCHAR(200),
    billing_email VARCHAR(254),
    default_document VARCHAR(50),
    exchange_rate NUMERIC(18,6),
    currency VARCHAR(3),
    invoice_by_post BOOLEAN NOT NULL DEFAULT FALSE,
    documents_by_email BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_customers_agency CHECK (agency IN ('LTFT01', 'LTFT02')),
    CONSTRAINT chk_customers_code_agency CHECK (
        (agency = 'LTFT01' AND customer_code LIKE '1_____') OR
        (agency = 'LTFT02' AND customer_code LIKE '2_____')
    ),
    CONSTRAINT chk_customers_type CHECK (customer_type IN ('COMPANY', 'PRIVATE'))
);

CREATE INDEX idx_customers_shipping_name ON customers (shipping_name);
CREATE INDEX idx_customers_active ON customers (active);

CREATE TABLE customer_code_counters (
    agency VARCHAR(10) PRIMARY KEY,
    next_number INTEGER NOT NULL CHECK (next_number BETWEEN 1 AND 100000)
);

INSERT INTO customer_code_counters (agency, next_number)
VALUES ('LTFT01', 1), ('LTFT02', 1);

CREATE TABLE recipients (
    id UUID PRIMARY KEY,
    deduplication_key VARCHAR(64) NOT NULL UNIQUE,
    code VARCHAR(30),
    designation VARCHAR(200) NOT NULL,
    contact_name VARCHAR(200),
    address VARCHAR(500) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    locality VARCHAR(120) NOT NULL,
    country VARCHAR(2) NOT NULL,
    email VARCHAR(254),
    phone VARCHAR(50),
    mobile VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ NOT NULL
);

CREATE UNIQUE INDEX uq_recipients_code ON recipients (code) WHERE code IS NOT NULL;
CREATE INDEX idx_recipients_designation ON recipients (designation);
CREATE INDEX idx_recipients_postal_code ON recipients (postal_code);
