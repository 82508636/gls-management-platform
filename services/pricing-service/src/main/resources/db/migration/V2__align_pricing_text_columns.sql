ALTER TABLE pricing_plans
    ALTER COLUMN currency TYPE VARCHAR(3);

ALTER TABLE pricing_routes
    ALTER COLUMN destination_country TYPE VARCHAR(2);
