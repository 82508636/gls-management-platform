CREATE TABLE service_groups (
    id VARCHAR(40) PRIMARY KEY,
    designation VARCHAR(160) NOT NULL UNIQUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE billing_zones (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    designation VARCHAR(160) NOT NULL,
    zone_type VARCHAR(40) NOT NULL,
    country VARCHAR(2) NOT NULL,
    group_name VARCHAR(120),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT billing_zones_type CHECK (zone_type IN ('DESTINATION_POSTAL_CODES'))
);

CREATE TABLE billing_zone_postal_codes (
    billing_zone_id UUID NOT NULL REFERENCES billing_zones(id) ON DELETE CASCADE,
    postal_code_pattern VARCHAR(30) NOT NULL,
    PRIMARY KEY (billing_zone_id, postal_code_pattern)
);

CREATE TABLE operational_services (
    id UUID PRIMARY KEY,
    code VARCHAR(40) NOT NULL UNIQUE,
    designation VARCHAR(200) NOT NULL,
    group_id VARCHAR(40) NOT NULL REFERENCES service_groups(id),
    transport_type VARCHAR(30) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    transit_min_hours INTEGER,
    transit_max_hours INTEGER,
    delivery_cutoff TIME,
    urgency SMALLINT NOT NULL,

    price_calculation_rule VARCHAR(30) NOT NULL,
    sales_calculation_rule VARCHAR(30) NOT NULL,
    forced_carrier VARCHAR(40),
    vat_mode VARCHAR(30) NOT NULL,
    price_per_volume BOOLEAN NOT NULL DEFAULT FALSE,
    price_per_cubic_meter BOOLEAN NOT NULL DEFAULT FALSE,
    price_per_dimensions BOOLEAN NOT NULL DEFAULT FALSE,
    price_by_bracket BOOLEAN NOT NULL DEFAULT FALSE,

    total_volumes_min INTEGER,
    total_volumes_max INTEGER,
    total_weight_min_kg NUMERIC(10,3),
    total_weight_max_kg NUMERIC(10,3),

    pickup_start TIME,
    pickup_end TIME,
    minimum_advance_minutes INTEGER,
    associated_pickup_service_id UUID REFERENCES operational_services(id) ON DELETE SET NULL,
    intercity_pickup_service_id UUID REFERENCES operational_services(id) ON DELETE SET NULL,

    pickup_only BOOLEAN NOT NULL DEFAULT FALSE,
    mail_vat_zero BOOLEAN NOT NULL DEFAULT FALSE,
    international BOOLEAN NOT NULL DEFAULT FALSE,
    sea_transport BOOLEAN NOT NULL DEFAULT FALSE,
    air_transport BOOLEAN NOT NULL DEFAULT FALSE,
    courier BOOLEAN NOT NULL DEFAULT FALSE,
    force_import BOOLEAN NOT NULL DEFAULT FALSE,
    force_export BOOLEAN NOT NULL DEFAULT FALSE,

    allows_cod BOOLEAN NOT NULL DEFAULT FALSE,
    allows_return BOOLEAN NOT NULL DEFAULT FALSE,
    allows_pudo BOOLEAN NOT NULL DEFAULT FALSE,
    requires_delivery_pin BOOLEAN NOT NULL DEFAULT FALSE,

    requires_email BOOLEAN NOT NULL DEFAULT FALSE,
    auto_submit_webservice BOOLEAN NOT NULL DEFAULT FALSE,
    requires_kilometres BOOLEAN NOT NULL DEFAULT FALSE,
    force_return BOOLEAN NOT NULL DEFAULT FALSE,
    no_pickup BOOLEAN NOT NULL DEFAULT FALSE,
    requires_dimensions BOOLEAN NOT NULL DEFAULT FALSE,
    insured_value BOOLEAN NOT NULL DEFAULT FALSE,
    map_identifier VARCHAR(120),

    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(120) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(120) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT operational_services_transport CHECK (transport_type IN ('LARGE_VOLUMES', 'SMALL_VOLUMES')),
    CONSTRAINT operational_services_urgency CHECK (urgency BETWEEN 1 AND 5),
    CONSTRAINT operational_services_price_rule CHECK (price_calculation_rule IN ('WEIGHT')),
    CONSTRAINT operational_services_sales_rule CHECK (sales_calculation_rule IN ('DEFAULT')),
    CONSTRAINT operational_services_vat_mode CHECK (vat_mode IN ('AUTO', 'STANDARD', 'ZERO', 'EXEMPT')),
    CONSTRAINT operational_services_transit CHECK (
        transit_min_hours IS NULL OR transit_max_hours IS NULL OR transit_max_hours >= transit_min_hours
    ),
    CONSTRAINT operational_services_total_volumes CHECK (
        (total_volumes_min IS NULL OR total_volumes_min >= 0) AND
        (total_volumes_max IS NULL OR total_volumes_max >= 0) AND
        (total_volumes_min IS NULL OR total_volumes_max IS NULL OR total_volumes_max >= total_volumes_min)
    ),
    CONSTRAINT operational_services_total_weight CHECK (
        (total_weight_min_kg IS NULL OR total_weight_min_kg >= 0) AND
        (total_weight_max_kg IS NULL OR total_weight_max_kg >= 0) AND
        (total_weight_min_kg IS NULL OR total_weight_max_kg IS NULL OR total_weight_max_kg >= total_weight_min_kg)
    ),
    CONSTRAINT operational_services_advance CHECK (minimum_advance_minutes IS NULL OR minimum_advance_minutes >= 0)
);

CREATE TABLE operational_service_zones (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL REFERENCES operational_services(id) ON DELETE CASCADE,
    billing_zone_id UUID NOT NULL REFERENCES billing_zones(id),
    transit_min_hours INTEGER,
    transit_max_hours INTEGER,
    CONSTRAINT operational_service_zone_unique UNIQUE (service_id, billing_zone_id),
    CONSTRAINT operational_service_zone_transit CHECK (
        transit_min_hours IS NULL OR transit_max_hours IS NULL OR transit_max_hours >= transit_min_hours
    )
);

CREATE TABLE operational_service_package_limits (
    id UUID PRIMARY KEY,
    service_id UUID NOT NULL REFERENCES operational_services(id) ON DELETE CASCADE,
    package_type VARCHAR(20) NOT NULL,
    max_weight_kg NUMERIC(10,3),
    max_length_cm NUMERIC(10,2),
    max_width_cm NUMERIC(10,2),
    max_height_cm NUMERIC(10,2),
    max_combined_cm NUMERIC(10,2),
    CONSTRAINT operational_service_package_unique UNIQUE (service_id, package_type),
    CONSTRAINT operational_service_package_type CHECK (package_type IN ('BOX', 'DOCUMENT', 'PALLET')),
    CONSTRAINT operational_service_package_positive CHECK (
        (max_weight_kg IS NULL OR max_weight_kg > 0) AND
        (max_length_cm IS NULL OR max_length_cm > 0) AND
        (max_width_cm IS NULL OR max_width_cm > 0) AND
        (max_height_cm IS NULL OR max_height_cm > 0) AND
        (max_combined_cm IS NULL OR max_combined_cm > 0)
    )
);

CREATE TABLE operational_service_pickup_days (
    service_id UUID NOT NULL REFERENCES operational_services(id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL,
    PRIMARY KEY (service_id, day_of_week),
    CONSTRAINT operational_service_pickup_day CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'))
);

CREATE TABLE operational_service_delivery_days (
    service_id UUID NOT NULL REFERENCES operational_services(id) ON DELETE CASCADE,
    day_of_week VARCHAR(10) NOT NULL,
    PRIMARY KEY (service_id, day_of_week),
    CONSTRAINT operational_service_delivery_day CHECK (day_of_week IN ('MONDAY','TUESDAY','WEDNESDAY','THURSDAY','FRIDAY','SATURDAY','SUNDAY'))
);

CREATE TABLE operational_service_customers (
    service_id UUID NOT NULL REFERENCES operational_services(id) ON DELETE CASCADE,
    customer_id UUID NOT NULL REFERENCES customers(id) ON DELETE CASCADE,
    PRIMARY KEY (service_id, customer_id)
);

CREATE TABLE operational_service_blocked_agencies (
    service_id UUID NOT NULL REFERENCES operational_services(id) ON DELETE CASCADE,
    agency VARCHAR(10) NOT NULL,
    PRIMARY KEY (service_id, agency),
    CONSTRAINT operational_service_agency CHECK (agency IN ('LTFT01', 'LTFT02'))
);

CREATE INDEX billing_zones_country_idx ON billing_zones(country, designation);
CREATE INDEX operational_services_group_idx ON operational_services(group_id, designation);
CREATE INDEX operational_services_active_idx ON operational_services(active, designation);
CREATE INDEX operational_service_customers_customer_idx ON operational_service_customers(customer_id);
