ALTER TABLE shipments
    DROP CONSTRAINT IF EXISTS shipments_pricing_route_id_fkey;

ALTER TABLE shipments
    ALTER COLUMN pricing_route_id DROP NOT NULL;

ALTER TABLE shipments
    ADD CONSTRAINT shipments_pricing_route_id_fkey
        FOREIGN KEY (pricing_route_id) REFERENCES pricing_routes(id) ON DELETE SET NULL;

ALTER TABLE shipments
    DROP CONSTRAINT IF EXISTS shipments_service_code;

ALTER TABLE shipments
    DROP COLUMN IF EXISTS service_code;

DROP INDEX IF EXISTS pricing_routes_plan_service_idx;

DELETE FROM pricing_routes;

UPDATE pricing_plans
SET code = CASE WHEN code = '4W-BE' AND version = 1 THEN 'LTFT-BASE' ELSE code END,
    designation = CASE WHEN code = '4W-BE' AND version = 1 THEN 'Tabela de preços LTFT' ELSE designation END,
    status = 'DRAFT',
    updated_at = current_timestamp,
    updated_by = 'Migração V15';

ALTER TABLE pricing_routes
    DROP CONSTRAINT IF EXISTS pricing_routes_service;

ALTER TABLE pricing_routes
    DROP COLUMN IF EXISTS service_code;

CREATE INDEX pricing_routes_plan_order_idx
    ON pricing_routes(pricing_plan_id, sort_order);
