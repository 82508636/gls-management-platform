package pt.glsmanagement.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

public final class CustomerDomainMigrator {
    private static final List<String> CUSTOMER_COLUMNS = List.of(
            "id", "customer_code", "abbreviation", "shipping_name", "agency", "address", "postal_code",
            "locality", "country", "contact_email", "mobile", "phone", "billing_country", "vat_number",
            "vat_key", "billing_legal_name", "billing_address", "billing_postal_code", "billing_locality",
            "account_code", "billing_reference", "customer_type", "responsible_name", "billing_email",
            "default_document", "exchange_rate", "currency", "invoice_by_post", "documents_by_email", "active",
            "created_at", "updated_at");
    private static final List<String> RECIPIENT_COLUMNS = List.of(
            "id", "deduplication_key", "code", "designation", "contact_name", "address", "postal_code",
            "locality", "country", "email", "phone", "mobile", "created_at", "updated_at", "last_used_at");

    private CustomerDomainMigrator() {}

    public static void main(String[] args) throws Exception {
        var settings = Settings.fromEnvironment(System.getenv());
        try (var source = DriverManager.getConnection(
                     settings.sourceUrl(), settings.sourceUsername(), settings.sourcePassword());
             var target = DriverManager.getConnection(
                     settings.targetUrl(), settings.targetUsername(), settings.targetPassword())) {
            var result = migrate(source, target);
            System.out.printf("CUSTOMER_DOMAIN_MIGRATED customers=%d recipients=%d%n",
                    result.customers(), result.recipients());
        }
    }

    static MigrationResult migrate(Connection source, Connection target) throws SQLException {
        target.setAutoCommit(false);
        try {
            acquireTargetLock(target);
            assertTargetIsEmpty(target);
            var sourceCustomers = count(source, "customers");
            var sourceRecipients = count(source, "recipients");
            var migratedCustomers = copy(source, target, "customers", CUSTOMER_COLUMNS);
            var migratedRecipients = copy(source, target, "recipients", RECIPIENT_COLUMNS);
            if (migratedCustomers != sourceCustomers || migratedRecipients != sourceRecipients) {
                throw new SQLException("The migrated row counts do not match the source database");
            }
            resetCustomerCounters(target);
            if (count(target, "customers") != sourceCustomers || count(target, "recipients") != sourceRecipients) {
                throw new SQLException("The target row counts do not match after migration");
            }
            target.commit();
            return new MigrationResult(migratedCustomers, migratedRecipients);
        } catch (Exception exception) {
            target.rollback();
            if (exception instanceof SQLException sqlException) throw sqlException;
            throw new SQLException("Customer domain migration failed", exception);
        } finally {
            target.setAutoCommit(true);
        }
    }

    private static int copy(Connection source, Connection target, String table, List<String> columns)
            throws SQLException {
        var columnList = String.join(", ", columns);
        var placeholders = String.join(", ", java.util.Collections.nCopies(columns.size(), "?"));
        var selectSql = "SELECT " + columnList + " FROM " + table + " ORDER BY id";
        var insertSql = "INSERT INTO " + table + " (" + columnList + ") VALUES (" + placeholders + ")";
        int copied = 0;
        try (var select = source.prepareStatement(selectSql);
             var rows = select.executeQuery();
             var insert = target.prepareStatement(insertSql)) {
            while (rows.next()) {
                for (int index = 1; index <= columns.size(); index++) insert.setObject(index, rows.getObject(index));
                insert.addBatch();
                copied++;
                if (copied % 250 == 0) insert.executeBatch();
            }
            insert.executeBatch();
        }
        return copied;
    }

    private static void acquireTargetLock(Connection target) throws SQLException {
        try (var statement = target.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            statement.setLong(1, 4_912_021L);
            statement.execute();
        }
    }

    private static void assertTargetIsEmpty(Connection target) throws SQLException {
        var customers = count(target, "customers");
        var recipients = count(target, "recipients");
        if (customers != 0 || recipients != 0) {
            throw new SQLException("Target database is not empty; migration was refused");
        }
    }

    private static int count(Connection connection, String table) throws SQLException {
        try (var statement = connection.prepareStatement("SELECT COUNT(*) FROM " + table);
             var result = statement.executeQuery()) {
            result.next();
            return result.getInt(1);
        }
    }

    private static void resetCustomerCounters(Connection target) throws SQLException {
        try (var statement = target.prepareStatement("""
                UPDATE customer_code_counters counter
                SET next_number = COALESCE((
                    SELECT MAX(CAST(SUBSTRING(customer.customer_code, 2) AS INTEGER)) + 1
                    FROM customers customer
                    WHERE customer.agency = counter.agency
                ), 1)
                """)) {
            statement.executeUpdate();
        }
    }

    record MigrationResult(int customers, int recipients) {}

    record Settings(
            String sourceUrl, String sourceUsername, String sourcePassword,
            String targetUrl, String targetUsername, String targetPassword) {
        static Settings fromEnvironment(Map<String, String> environment) {
            return new Settings(
                    required(environment, "SOURCE_DATABASE_URL"),
                    required(environment, "SOURCE_DATABASE_USERNAME"),
                    required(environment, "SOURCE_DATABASE_PASSWORD"),
                    required(environment, "TARGET_DATABASE_URL"),
                    required(environment, "TARGET_DATABASE_USERNAME"),
                    required(environment, "TARGET_DATABASE_PASSWORD"));
        }

        private static String required(Map<String, String> environment, String name) {
            var value = environment.get(name);
            if (value == null || value.isBlank()) throw new IllegalArgumentException(name + " is required");
            return value;
        }
    }
}
