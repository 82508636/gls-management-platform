CREATE TABLE identity_access_states (
    user_id VARCHAR(100) PRIMARY KEY,
    expected_role VARCHAR(30),
    enabled BOOLEAN NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_identity_access_role CHECK (
        expected_role IS NULL OR expected_role IN ('ADMIN', 'OPERATOR', 'ACCOUNTING', 'CUSTOMER', 'DRIVER', 'FRONT_DESK')
    )
);

CREATE TABLE identity_audit_events (
    id UUID PRIMARY KEY,
    occurred_at TIMESTAMPTZ NOT NULL,
    actor_subject VARCHAR(100) NOT NULL,
    actor_username VARCHAR(200),
    action VARCHAR(20) NOT NULL,
    target_user_id VARCHAR(100) NOT NULL,
    target_username VARCHAR(200) NOT NULL,
    previous_roles VARCHAR(500),
    new_roles VARCHAR(500),
    details VARCHAR(500)
);

CREATE INDEX idx_identity_audit_target ON identity_audit_events (target_user_id, occurred_at DESC);
