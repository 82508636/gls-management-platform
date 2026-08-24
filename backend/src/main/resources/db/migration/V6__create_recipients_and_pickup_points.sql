CREATE TABLE recipients (
    id UUID PRIMARY KEY,
    deduplication_key VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    contact_name VARCHAR(200),
    address VARCHAR(500) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    locality VARCHAR(120) NOT NULL,
    country VARCHAR(2) NOT NULL,
    email VARCHAR(254),
    phone VARCHAR(50),
    mobile VARCHAR(50),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    last_used_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_recipients_name ON recipients (name);
CREATE INDEX idx_recipients_postal_code ON recipients (postal_code);

CREATE TABLE pickup_points (
    id UUID PRIMARY KEY,
    code VARCHAR(30) NOT NULL UNIQUE,
    designation VARCHAR(200) NOT NULL,
    supplier VARCHAR(100) NOT NULL,
    morning_open TIME,
    morning_close TIME,
    afternoon_open TIME,
    afternoon_close TIME,
    address VARCHAR(500) NOT NULL,
    postal_code VARCHAR(20) NOT NULL,
    locality VARCHAR(120) NOT NULL,
    country VARCHAR(2) NOT NULL,
    email VARCHAR(254),
    phone VARCHAR(50),
    mobile VARCHAR(50),
    open_saturday BOOLEAN NOT NULL DEFAULT FALSE,
    open_sunday BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_pickup_points_designation ON pickup_points (designation);
CREATE INDEX idx_pickup_points_locality ON pickup_points (locality);
