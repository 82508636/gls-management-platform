CREATE TABLE customers (
    id UUID PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    vat_number VARCHAR(50) NOT NULL UNIQUE,
    email VARCHAR(254),
    phone VARCHAR(50),
    billing_address VARCHAR(500),
    gls_account VARCHAR(100),
    gls_customer_reference VARCHAR(100),
    pricing_plan_id UUID,
    payment_terms INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_customers_gls_account ON customers (gls_account);
CREATE INDEX idx_customers_gls_customer_reference ON customers (gls_customer_reference);

