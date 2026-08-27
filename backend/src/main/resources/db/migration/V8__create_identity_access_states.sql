CREATE TABLE identity_access_states (
    user_id VARCHAR(100) PRIMARY KEY,
    expected_role VARCHAR(30),
    enabled BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_identity_access_role CHECK (
        expected_role IS NULL OR expected_role IN ('ADMIN', 'OPERATOR', 'ACCOUNTING', 'CUSTOMER', 'DRIVER', 'FRONT_DESK')
    )
);
