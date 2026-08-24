BEGIN;

LOCK TABLE customer_code_counters IN ROW EXCLUSIVE MODE;

DELETE FROM customers
WHERE billing_reference LIKE 'FAKE-SEED-%';

DO $$
DECLARE
    start_ltft01 INTEGER;
    start_ltft02 INTEGER;
BEGIN
    SELECT COALESCE(MAX(SUBSTRING(customer_code FROM 2)::INTEGER) + 1, 1)
    INTO start_ltft01
    FROM customers
    WHERE agency = 'LTFT01';

    SELECT COALESCE(MAX(SUBSTRING(customer_code FROM 2)::INTEGER) + 1, 1)
    INTO start_ltft02
    FROM customers
    WHERE agency = 'LTFT02';

    IF start_ltft01 + 99 > 99999 OR start_ltft02 + 99 > 99999 THEN
        RAISE EXCEPTION 'Not enough customer codes available for the local fake dataset';
    END IF;

    WITH source AS (
        SELECT
            number,
            CASE WHEN number <= 100 THEN 'LTFT01' ELSE 'LTFT02' END AS agency,
            CASE WHEN number <= 100 THEN number ELSE number - 100 END AS agency_position,
            (59000000 + number)::TEXT AS vat_base
        FROM generate_series(1, 200) AS number
    ), calculated AS (
        SELECT source.*,
            (
                SELECT SUM(SUBSTRING(source.vat_base FROM digit FOR 1)::INTEGER * (10 - digit))
                FROM generate_series(1, 8) AS digit
            ) AS vat_sum
        FROM source
    ), fake_customers AS (
        SELECT calculated.*,
            vat_base || CASE
                WHEN 11 - (vat_sum % 11) >= 10 THEN '0'
                ELSE (11 - (vat_sum % 11))::TEXT
            END AS vat_number
        FROM calculated
    )
    INSERT INTO customers (
        id, customer_code, abbreviation, shipping_name, agency,
        address, postal_code, locality, country, contact_email, mobile, phone,
        billing_country, vat_number, billing_legal_name, billing_address,
        billing_postal_code, billing_locality, account_code, billing_reference,
        customer_type, responsible_name, billing_email, default_document,
        exchange_rate, currency, invoice_by_post, documents_by_email, active,
        created_at, updated_at
    )
    SELECT
        gen_random_uuid(),
        CASE fake_customers.agency
            WHEN 'LTFT01' THEN '1' || LPAD((start_ltft01 + agency_position - 1)::TEXT, 5, '0')
            ELSE '2' || LPAD((start_ltft02 + agency_position - 1)::TEXT, 5, '0')
        END,
        'CT' || LPAD(number::TEXT, 3, '0'),
        'Cliente Teste ' || LPAD(number::TEXT, 3, '0') || ', Lda.',
        fake_customers.agency,
        'Rua de Teste ' || number || ', ' || (10 + number % 80),
        CASE fake_customers.agency WHEN 'LTFT01' THEN '4820-' ELSE '4805-' END || LPAD((number % 999)::TEXT, 3, '0'),
        CASE fake_customers.agency WHEN 'LTFT01' THEN 'Fafe' ELSE 'Guimarães' END,
        'PT',
        'cliente.teste.' || LPAD(number::TEXT, 3, '0') || '@example.test',
        '91' || LPAD((1000000 + number)::TEXT, 7, '0'),
        '25' || LPAD((3000000 + number)::TEXT, 7, '0'),
        'PT',
        fake_customers.vat_number,
        'Cliente Teste ' || LPAD(number::TEXT, 3, '0') || ', Lda.',
        'Rua de Faturação ' || number || ', ' || (10 + number % 80),
        CASE fake_customers.agency WHEN 'LTFT01' THEN '4820-' ELSE '4805-' END || LPAD((number % 999)::TEXT, 3, '0'),
        CASE fake_customers.agency WHEN 'LTFT01' THEN 'Fafe' ELSE 'Guimarães' END,
        'FAKE-' || LPAD(number::TEXT, 4, '0'),
        'FAKE-SEED-' || LPAD(number::TEXT, 4, '0'),
        'COMPANY',
        'Responsável Teste ' || LPAD(number::TEXT, 3, '0'),
        'faturacao.teste.' || LPAD(number::TEXT, 3, '0') || '@example.test',
        'INVOICE',
        1.000000,
        'EUR',
        number % 5 = 0,
        TRUE,
        number % 5 <> 0,
        CURRENT_TIMESTAMP - ((200 - number) || ' minutes')::INTERVAL,
        CURRENT_TIMESTAMP
    FROM fake_customers;

    UPDATE customer_code_counters
    SET next_number = start_ltft01 + 100
    WHERE agency = 'LTFT01';

    UPDATE customer_code_counters
    SET next_number = start_ltft02 + 100
    WHERE agency = 'LTFT02';
END $$;

COMMIT;
