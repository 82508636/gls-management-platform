CREATE TABLE pricing_plans (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL,
    designation VARCHAR(160) NOT NULL,
    version INTEGER NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE,
    currency CHAR(3) NOT NULL DEFAULT 'EUR',
    fuel_surcharge_percent NUMERIC(7, 4) NOT NULL DEFAULT 0,
    vat_percent NUMERIC(7, 4) NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    CONSTRAINT pricing_plans_code_version_key UNIQUE (code, version),
    CONSTRAINT pricing_plans_version_positive CHECK (version > 0),
    CONSTRAINT pricing_plans_validity CHECK (valid_to IS NULL OR valid_to >= valid_from),
    CONSTRAINT pricing_plans_fuel_percent CHECK (fuel_surcharge_percent BETWEEN 0 AND 100),
    CONSTRAINT pricing_plans_vat_percent CHECK (vat_percent BETWEEN 0 AND 100),
    CONSTRAINT pricing_plans_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE TABLE pricing_routes (
    id UUID PRIMARY KEY,
    pricing_plan_id UUID NOT NULL REFERENCES pricing_plans(id) ON DELETE CASCADE,
    code VARCHAR(60) NOT NULL,
    designation VARCHAR(160) NOT NULL,
    destination_country CHAR(2) NOT NULL,
    delivery_commitment VARCHAR(40) NOT NULL,
    volumetric_factor NUMERIC(8, 3) NOT NULL,
    max_piece_weight_kg NUMERIC(8, 3) NOT NULL,
    max_combined_dimensions_cm NUMERIC(8, 2),
    additional_step_kg NUMERIC(8, 3) NOT NULL DEFAULT 1,
    additional_step_price NUMERIC(12, 2),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL,
    CONSTRAINT pricing_routes_plan_code_key UNIQUE (pricing_plan_id, code),
    CONSTRAINT pricing_routes_positive_values CHECK (
        volumetric_factor > 0 AND max_piece_weight_kg > 0 AND additional_step_kg > 0
    ),
    CONSTRAINT pricing_routes_additional_price CHECK (
        additional_step_price IS NULL OR additional_step_price >= 0
    )
);

CREATE TABLE pricing_brackets (
    id UUID PRIMARY KEY,
    pricing_route_id UUID NOT NULL REFERENCES pricing_routes(id) ON DELETE CASCADE,
    up_to_weight_kg NUMERIC(8, 3) NOT NULL,
    price NUMERIC(12, 2) NOT NULL,
    sort_order INTEGER NOT NULL,
    CONSTRAINT pricing_brackets_route_weight_key UNIQUE (pricing_route_id, up_to_weight_kg),
    CONSTRAINT pricing_brackets_positive_values CHECK (up_to_weight_kg > 0 AND price >= 0)
);

CREATE INDEX pricing_routes_plan_order_idx ON pricing_routes(pricing_plan_id, sort_order);
CREATE INDEX pricing_brackets_route_order_idx ON pricing_brackets(pricing_route_id, up_to_weight_kg);
