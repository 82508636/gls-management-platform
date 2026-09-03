CREATE TABLE pickup_points (
    id UUID PRIMARY KEY,
    code VARCHAR(30) NOT NULL,
    designation VARCHAR(200) NOT NULL,
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
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_pickup_points_code UNIQUE (code),
    CONSTRAINT chk_pickup_points_country CHECK (country ~ '^[A-Z]{2}$'),
    CONSTRAINT chk_pickup_morning_hours CHECK (
        (morning_open IS NULL AND morning_close IS NULL)
        OR (morning_open IS NOT NULL AND morning_close IS NOT NULL AND morning_open < morning_close)
    ),
    CONSTRAINT chk_pickup_afternoon_hours CHECK (
        (afternoon_open IS NULL AND afternoon_close IS NULL)
        OR (afternoon_open IS NOT NULL AND afternoon_close IS NOT NULL AND afternoon_open < afternoon_close)
    ),
    CONSTRAINT chk_pickup_non_overlapping_hours CHECK (
        morning_close IS NULL OR afternoon_open IS NULL OR morning_close <= afternoon_open
    )
);

CREATE INDEX idx_pickup_points_designation ON pickup_points (designation);
CREATE INDEX idx_pickup_points_active ON pickup_points (active);
