BEGIN;

DELETE FROM shipments WHERE external_reference LIKE 'DEMO-%';
DELETE FROM recipients WHERE code LIKE 'DEMO-%';

WITH destinations(code, designation, contact_name, address, postal_code, locality, country, email, phone) AS (
    VALUES
        ('DEMO-LIS', 'Loja Lisboa Centro', 'Ana Sousa', 'Avenida da República, 45', '1050-187', 'Lisboa', 'PT', 'lisboa.destino@example.test', '211000001'),
        ('DEMO-POR', 'Armazém Porto Norte', 'Bruno Costa', 'Rua de Camões, 310', '4000-142', 'Porto', 'PT', 'porto.destino@example.test', '221000002'),
        ('DEMO-MAD', 'Comercio Madrid Central', 'Carmen Ruiz', 'Calle de Alcalá, 120', '28009', 'Madrid', 'ES', 'madrid.destino@example.test', '34910000003'),
        ('DEMO-BCN', 'Distribución Barcelona', 'Daniel Serra', 'Carrer de Mallorca, 245', '08008', 'Barcelona', 'ES', 'barcelona.destino@example.test', '34930000004'),
        ('DEMO-SEV', 'Servicios Sevilla Sur', 'Elena Pérez', 'Avenida de la Constitución, 18', '41004', 'Sevilla', 'ES', 'sevilla.destino@example.test', '34950000005'),
        ('DEMO-VLC', 'Logística Valencia Este', 'Francisco Martí', 'Carrer de Colón, 62', '46004', 'Valencia', 'ES', 'valencia.destino@example.test', '34960000006')
)
INSERT INTO recipients (
    id, deduplication_key, code, designation, contact_name, address, postal_code, locality, country,
    email, phone, mobile, created_at, updated_at, last_used_at
)
SELECT md5('demo-recipient-' || code)::uuid,
       md5('demo-recipient-key-' || code) || md5('demo-recipient-key-2-' || code),
       code, designation, contact_name, address, postal_code, locality, country,
       email, phone, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM destinations;

WITH demo(position, recipient_code, route_code, shipment_date, due_date, parcel_count, weight, payment_status) AS (
    VALUES
        (1, 'DEMO-LIS', 'BUS_PT_24H', DATE '2026-08-01', DATE '2026-08-31', 1,  4.000::numeric, 'PENDING'),
        (2, 'DEMO-POR', 'BUS_PT_24H', DATE '2026-08-05', DATE '2026-09-04', 1, 12.000::numeric, 'PENDING'),
        (3, 'DEMO-MAD', 'BUS_ES_48H', DATE '2026-07-22', DATE '2026-08-21', 1,  8.000::numeric, 'PAID'),
        (4, 'DEMO-BCN', 'EXP_ES_1900', DATE '2026-08-08', DATE '2026-09-07', 2, 32.100::numeric, 'PENDING'),
        (5, 'DEMO-SEV', 'EXP_ES_1400', DATE '2026-07-28', DATE '2026-08-27', 1,  9.000::numeric, 'PAID'),
        (6, 'DEMO-VLC', 'EXP_ES_1000', DATE '2026-08-12', DATE '2026-09-11', 1, 12.000::numeric, 'PENDING')
), calculated AS (
    SELECT demo.*, customer.id AS customer_id, customer.customer_code, customer.shipping_name,
           recipient.id AS recipient_id, recipient.designation AS recipient_designation,
           recipient.address AS recipient_address, recipient.postal_code AS recipient_postal_code,
           recipient.locality AS recipient_locality, recipient.country AS recipient_country,
           plan.id AS plan_id, plan.code AS plan_code, plan.version AS plan_version,
           plan.currency, plan.fuel_surcharge_percent, plan.vat_percent,
           route.id AS route_id, route.designation AS route_designation, route.service_code,
           route.additional_step_kg, route.additional_step_price,
           bracket.up_to_weight_kg, bracket.price AS bracket_price
    FROM demo
    JOIN customers customer
      ON customer.billing_reference = 'FAKE-SEED-' || LPAD(demo.position::text, 4, '0')
    JOIN recipients recipient ON recipient.code = demo.recipient_code
    JOIN pricing_plans plan ON plan.code = '4W-BE' AND plan.version = 1
    JOIN pricing_routes route ON route.pricing_plan_id = plan.id AND route.code = demo.route_code
    JOIN LATERAL (
        SELECT value.up_to_weight_kg, value.price
        FROM pricing_brackets value
        WHERE value.pricing_route_id = route.id
        ORDER BY (value.up_to_weight_kg < demo.weight),
                 CASE WHEN value.up_to_weight_kg >= demo.weight THEN value.up_to_weight_kg END ASC,
                 CASE WHEN value.up_to_weight_kg < demo.weight THEN value.up_to_weight_kg END DESC
        LIMIT 1
    ) bracket ON TRUE
), priced AS (
    SELECT calculated.*,
           ROUND(bracket_price + CASE WHEN weight > up_to_weight_kg
               THEN CEIL((weight - up_to_weight_kg) / additional_step_kg) * additional_step_price
               ELSE 0 END, 2) AS base_price
    FROM calculated
), totals AS (
    SELECT priced.*, ROUND(base_price * fuel_surcharge_percent / 100, 2) AS fuel_surcharge
    FROM priced
), final AS (
    SELECT totals.*, ROUND(base_price + fuel_surcharge, 2) AS subtotal
    FROM totals
)
INSERT INTO shipments (
    id, shipment_number, external_reference, customer_id, customer_code, customer_name,
    recipient_id, recipient_code, recipient_designation, recipient_address, recipient_postal_code,
    recipient_locality, recipient_country, pricing_plan_id, pricing_plan_code, pricing_plan_version,
    pricing_route_id, route_code, route_designation, service_code, shipment_date, due_date,
    parcel_count, actual_weight_kg, volumetric_weight_kg, chargeable_weight_kg,
    length_cm, width_cm, height_cm, base_price, fuel_surcharge, subtotal, vat, total, currency,
    shipment_status, payment_status, paid_at, created_at, created_by
)
SELECT md5('demo-shipment-' || position)::uuid,
       'DEMO-2026-' || LPAD(position::text, 6, '0'),
       'DEMO-' || LPAD(position::text, 6, '0'),
       customer_id, customer_code, shipping_name,
       recipient_id, recipient_code, recipient_designation, recipient_address, recipient_postal_code,
       recipient_locality, recipient_country, plan_id, plan_code, plan_version,
       route_id, route_code, route_designation, service_code, shipment_date, due_date,
       parcel_count, weight, 0.000, weight,
       NULL, NULL, NULL, base_price, fuel_surcharge, subtotal,
       ROUND(subtotal * vat_percent / 100, 2),
       ROUND(subtotal + ROUND(subtotal * vat_percent / 100, 2), 2), currency,
       CASE WHEN payment_status = 'PAID' THEN 'DELIVERED' ELSE 'CREATED' END,
       payment_status,
       CASE WHEN payment_status = 'PAID' THEN due_date::timestamp with time zone - INTERVAL '3 days' ELSE NULL END,
       shipment_date::timestamp with time zone + INTERVAL '9 hours', 'seed-local-demo-shipments'
FROM final;

COMMIT;
