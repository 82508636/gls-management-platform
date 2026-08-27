CREATE TABLE account_profiles (
    id VARCHAR(40) PRIMARY KEY,
    designation VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(200) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(200) NOT NULL
);

CREATE TABLE professional_categories (
    id VARCHAR(40) PRIMARY KEY,
    designation VARCHAR(120) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_by VARCHAR(200) NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_by VARCHAR(200) NOT NULL
);

INSERT INTO account_profiles (id, designation, active, created_at, created_by, updated_at, updated_by) VALUES
    ('ADMIN', 'Administrador', TRUE, CURRENT_TIMESTAMP, 'Sistema', CURRENT_TIMESTAMP, 'Sistema'),
    ('OPERATOR', 'Operador', TRUE, CURRENT_TIMESTAMP, 'Sistema', CURRENT_TIMESTAMP, 'Sistema'),
    ('ACCOUNTING', 'Contabilidade', TRUE, CURRENT_TIMESTAMP, 'Sistema', CURRENT_TIMESTAMP, 'Sistema'),
    ('DRIVER', 'Motorista', TRUE, CURRENT_TIMESTAMP, 'Sistema', CURRENT_TIMESTAMP, 'Sistema'),
    ('FRONT_DESK', 'Atendedor de Balcão', TRUE, CURRENT_TIMESTAMP, 'Sistema', CURRENT_TIMESTAMP, 'Sistema');

INSERT INTO professional_categories (id, designation, active, created_at, created_by, updated_at, updated_by) VALUES
    ('1', 'Administrativo', TRUE, CURRENT_TIMESTAMP, 'Sistema', CURRENT_TIMESTAMP, 'Sistema'),
    ('2', 'Motoristas Ligeiros', TRUE, CURRENT_TIMESTAMP, 'Sistema', CURRENT_TIMESTAMP, 'Sistema'),
    ('3', 'Motoristas Pesados', TRUE, CURRENT_TIMESTAMP, 'Sistema', CURRENT_TIMESTAMP, 'Sistema'),
    ('4', 'Operadores de Armazém', TRUE, CURRENT_TIMESTAMP, 'Sistema', CURRENT_TIMESTAMP, 'Sistema'),
    ('5', 'Oficina', TRUE, CURRENT_TIMESTAMP, 'Sistema', CURRENT_TIMESTAMP, 'Sistema'),
    ('6', 'Outros', TRUE, CURRENT_TIMESTAMP, 'Sistema', CURRENT_TIMESTAMP, 'Sistema');

CREATE UNIQUE INDEX uq_account_profiles_designation_ci ON account_profiles (LOWER(designation));
CREATE UNIQUE INDEX uq_professional_categories_designation_ci ON professional_categories (LOWER(designation));
