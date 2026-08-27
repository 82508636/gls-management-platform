create table pricing_plans (
    id uuid primary key,
    code varchar(40) not null,
    designation varchar(160) not null,
    version integer not null,
    valid_from date not null,
    valid_to date,
    currency char(3) not null default 'EUR',
    fuel_surcharge_percent numeric(7, 4) not null default 0,
    vat_percent numeric(7, 4) not null default 0,
    status varchar(20) not null,
    created_at timestamp with time zone not null,
    created_by varchar(120) not null,
    updated_at timestamp with time zone not null,
    updated_by varchar(120) not null,
    constraint pricing_plans_code_version_key unique (code, version),
    constraint pricing_plans_version_positive check (version > 0),
    constraint pricing_plans_validity check (valid_to is null or valid_to >= valid_from),
    constraint pricing_plans_fuel_percent check (fuel_surcharge_percent between 0 and 100),
    constraint pricing_plans_vat_percent check (vat_percent between 0 and 100),
    constraint pricing_plans_status check (status in ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

create table pricing_routes (
    id uuid primary key,
    pricing_plan_id uuid not null references pricing_plans(id) on delete cascade,
    service_code varchar(30) not null,
    code varchar(60) not null,
    designation varchar(160) not null,
    destination_country char(2) not null,
    delivery_commitment varchar(40) not null,
    volumetric_factor numeric(8, 3) not null,
    max_piece_weight_kg numeric(8, 3) not null,
    max_combined_dimensions_cm numeric(8, 2),
    additional_step_kg numeric(8, 3) not null default 1,
    additional_step_price numeric(12, 2),
    enabled boolean not null default true,
    sort_order integer not null,
    constraint pricing_routes_plan_code_key unique (pricing_plan_id, code),
    constraint pricing_routes_service check (service_code in ('BUSINESS_PARCEL', 'EXPRESS_PARCEL')),
    constraint pricing_routes_positive_values check (
        volumetric_factor > 0 and max_piece_weight_kg > 0 and additional_step_kg > 0
    ),
    constraint pricing_routes_additional_price check (additional_step_price is null or additional_step_price >= 0)
);

create table pricing_brackets (
    id uuid primary key,
    pricing_route_id uuid not null references pricing_routes(id) on delete cascade,
    up_to_weight_kg numeric(8, 3) not null,
    price numeric(12, 2) not null,
    sort_order integer not null,
    constraint pricing_brackets_route_weight_key unique (pricing_route_id, up_to_weight_kg),
    constraint pricing_brackets_positive_values check (up_to_weight_kg > 0 and price >= 0)
);

create index pricing_routes_plan_service_idx on pricing_routes(pricing_plan_id, service_code, sort_order);
create index pricing_brackets_route_order_idx on pricing_brackets(pricing_route_id, up_to_weight_kg);

insert into pricing_plans (
    id, code, designation, version, valid_from, valid_to, currency,
    fuel_surcharge_percent, vat_percent, status, created_at, created_by, updated_at, updated_by
) values (
    md5('4-winners-business-express-2025-v1')::uuid,
    '4W-BE', '4 Winners - Business e Express', 1,
    date '2026-05-18', date '2026-08-17', 'EUR', 7.0000, 23.0000, 'DRAFT',
    current_timestamp, 'Migração V12', current_timestamp, 'Migração V12'
);

with plan as (
    select id from pricing_plans where code = '4W-BE' and version = 1
), route_data(service_code, code, designation, country, commitment, factor, max_weight, extra_price, position) as (
    values
      ('BUSINESS_PARCEL', 'BUS_PT_24H', 'Portugal 24h', 'PT', '24h', 167.000, 40.000, 0.36, 10),
      ('BUSINESS_PARCEL', 'BUS_ES_48H', 'Espanha 48h', 'ES', '48h', 167.000, 40.000, 0.61, 20),
      ('BUSINESS_PARCEL', 'BUS_MADEIRA_AIR', 'Madeira / São Miguel aéreo', 'PT', '24-48h', 250.000, 40.000, 3.49, 30),
      ('BUSINESS_PARCEL', 'BUS_OTHER_ISLANDS_AIR', 'Restantes ilhas aéreo', 'PT', '48-96h', 250.000, 40.000, 3.88, 40),
      ('EXPRESS_PARCEL', 'EXP_PT_14H', 'Portugal 14h', 'PT', '14h', 167.000, 40.000, 0.32, 110),
      ('EXPRESS_PARCEL', 'EXP_ES_0830', 'Espanha 08:30', 'ES', '08:30', 167.000, 40.000, 1.16, 120),
      ('EXPRESS_PARCEL', 'EXP_ES_1000', 'Espanha 10:00', 'ES', '10:00', 167.000, 40.000, 1.05, 130),
      ('EXPRESS_PARCEL', 'EXP_ES_1400', 'Espanha 14h', 'ES', '14h', 167.000, 40.000, 0.97, 140),
      ('EXPRESS_PARCEL', 'EXP_ES_1900', 'Espanha 19h', 'ES', '19h', 167.000, 40.000, 0.65, 150),
      ('EXPRESS_PARCEL', 'EXP_BALEARES_MAYORES', 'Baleares Maiores', 'ES', '24/48h', 250.000, 40.000, 1.85, 160),
      ('EXPRESS_PARCEL', 'EXP_BALEARES_MENORES', 'Baleares Menores', 'ES', '24/48h', 250.000, 40.000, 2.00, 170),
      ('EXPRESS_PARCEL', 'EXP_CANARIAS_MAYORES', 'Canárias Maiores', 'ES', '24/48h', 250.000, 40.000, 2.15, 180),
      ('EXPRESS_PARCEL', 'EXP_CANARIAS_MENORES', 'Canárias Menores', 'ES', '24/48h', 250.000, 40.000, 3.20, 190),
      ('EXPRESS_PARCEL', 'EXP_CEUTA_MELILLA', 'Ceuta / Melilla', 'ES', '24/48h', 250.000, 40.000, 3.31, 200),
      ('EXPRESS_PARCEL', 'EXP_GIBRALTAR', 'Gibraltar', 'GI', '24/48h', 250.000, 40.000, 2.20, 210),
      ('EXPRESS_PARCEL', 'EXP_ANDORRA', 'Andorra', 'AD', '24/48h', 250.000, 40.000, 2.20, 220)
)
insert into pricing_routes (
    id, pricing_plan_id, service_code, code, designation, destination_country,
    delivery_commitment, volumetric_factor, max_piece_weight_kg,
    max_combined_dimensions_cm, additional_step_kg, additional_step_price, enabled, sort_order
)
select md5('pricing-route-' || route_data.code)::uuid, plan.id, service_code, route_data.code,
       designation, country, commitment, factor, max_weight, 300.00, 1.000, extra_price, true, position
from route_data cross join plan;

with bracket_data(route_code, weight, price, position) as (
    values
      ('BUS_PT_24H',1,4.10,1),('BUS_PT_24H',3,4.10,2),('BUS_PT_24H',5,4.56,3),('BUS_PT_24H',10,5.49,4),('BUS_PT_24H',15,6.55,5),('BUS_PT_24H',20,7.49,6),('BUS_PT_24H',25,8.71,7),('BUS_PT_24H',30,9.52,8),
      ('BUS_ES_48H',1,6.07,1),('BUS_ES_48H',3,6.07,2),('BUS_ES_48H',5,6.89,3),('BUS_ES_48H',10,8.42,4),('BUS_ES_48H',15,9.59,5),('BUS_ES_48H',20,10.68,6),('BUS_ES_48H',25,12.60,7),('BUS_ES_48H',30,14.04,8),
      ('BUS_MADEIRA_AIR',1,16.04,1),('BUS_OTHER_ISLANDS_AIR',1,16.90,1),
      ('EXP_PT_14H',1,6.11,1),('EXP_PT_14H',3,6.70,2),('EXP_PT_14H',5,7.14,3),('EXP_PT_14H',10,8.35,4),('EXP_PT_14H',15,10.00,5),('EXP_PT_14H',20,10.88,6),('EXP_PT_14H',25,12.50,7),('EXP_PT_14H',30,13.25,8),
      ('EXP_ES_0830',1,15.10,1),('EXP_ES_0830',3,17.41,2),('EXP_ES_0830',5,19.70,3),('EXP_ES_0830',10,25.47,4),('EXP_ES_0830',15,31.21,5),
      ('EXP_ES_1000',1,9.28,1),('EXP_ES_1000',3,9.49,2),('EXP_ES_1000',5,10.86,3),('EXP_ES_1000',10,13.12,4),('EXP_ES_1000',15,17.87,5),
      ('EXP_ES_1400',1,7.60,1),('EXP_ES_1400',3,7.91,2),('EXP_ES_1400',5,8.89,3),('EXP_ES_1400',10,10.46,4),('EXP_ES_1400',15,14.62,5),
      ('EXP_ES_1900',1,6.44,1),('EXP_ES_1900',3,6.44,2),('EXP_ES_1900',5,7.29,3),('EXP_ES_1900',10,8.93,4),('EXP_ES_1900',15,10.17,5),('EXP_ES_1900',20,11.32,6),('EXP_ES_1900',25,13.35,7),('EXP_ES_1900',30,14.89,8),
      ('EXP_BALEARES_MAYORES',1,15.00,1),('EXP_BALEARES_MENORES',1,15.00,1),('EXP_CANARIAS_MAYORES',1,11.00,1),('EXP_CANARIAS_MENORES',1,15.00,1),('EXP_CEUTA_MELILLA',1,15.00,1),('EXP_GIBRALTAR',1,15.00,1),('EXP_ANDORRA',1,12.00,1)
)
insert into pricing_brackets (id, pricing_route_id, up_to_weight_kg, price, sort_order)
select md5('pricing-bracket-' || bracket_data.route_code || '-' || bracket_data.weight)::uuid,
       route.id, bracket_data.weight, bracket_data.price, bracket_data.position
from bracket_data
join pricing_routes route on route.code = bracket_data.route_code
join pricing_plans plan on plan.id = route.pricing_plan_id and plan.code = '4W-BE' and plan.version = 1;
