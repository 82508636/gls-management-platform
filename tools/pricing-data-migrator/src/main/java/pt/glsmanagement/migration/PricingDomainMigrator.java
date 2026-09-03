package pt.glsmanagement.migration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public final class PricingDomainMigrator {
    private static final List<String> PLAN_COLUMNS = List.of(
            "id", "code", "designation", "version", "valid_from", "valid_to", "currency",
            "fuel_surcharge_percent", "vat_percent", "status", "created_at", "created_by",
            "updated_at", "updated_by");
    private static final List<String> ROUTE_COLUMNS = List.of(
            "id", "pricing_plan_id", "code", "designation", "destination_country",
            "delivery_commitment", "volumetric_factor", "max_piece_weight_kg",
            "max_combined_dimensions_cm", "additional_step_kg", "additional_step_price",
            "enabled", "sort_order");
    private static final List<String> BRACKET_COLUMNS = List.of(
            "id", "pricing_route_id", "up_to_weight_kg", "price", "sort_order");

    private PricingDomainMigrator() {}

    public static void main(String[] args) throws Exception {
        var settings = Settings.fromEnvironment(System.getenv());
        try (var source = DriverManager.getConnection(
                     settings.sourceUrl(), settings.sourceUsername(), settings.sourcePassword());
             var target = DriverManager.getConnection(
                     settings.targetUrl(), settings.targetUsername(), settings.targetPassword())) {
            var result = migrate(source, target);
            System.out.printf("PRICING_DOMAIN_MIGRATED plans=%d routes=%d brackets=%d%n",
                    result.plans(), result.routes(), result.brackets());
        }
    }

    static MigrationResult migrate(Connection source, Connection target) throws SQLException {
        target.setAutoCommit(false);
        try {
            acquireTargetLock(target);
            assertTargetIsEmpty(target);
            var sourcePlans = count(source, "pricing_plans");
            var sourceRoutes = count(source, "pricing_routes");
            var sourceBrackets = count(source, "pricing_brackets");
            var plans = copy(source, target, "pricing_plans", PLAN_COLUMNS);
            var routes = copy(source, target, "pricing_routes", ROUTE_COLUMNS);
            var brackets = copy(source, target, "pricing_brackets", BRACKET_COLUMNS);
            if (plans != sourcePlans || routes != sourceRoutes || brackets != sourceBrackets) {
                throw new SQLException("The migrated row counts do not match the source database");
            }
            if (count(target, "pricing_plans") != sourcePlans
                    || count(target, "pricing_routes") != sourceRoutes
                    || count(target, "pricing_brackets") != sourceBrackets) {
                throw new SQLException("The target row counts do not match after migration");
            }
            assertSameIds(source, target, "pricing_plans");
            assertSameIds(source, target, "pricing_routes");
            assertSameIds(source, target, "pricing_brackets");
            target.commit();
            return new MigrationResult(plans, routes, brackets);
        } catch (Exception exception) {
            target.rollback();
            if (exception instanceof SQLException sqlException) throw sqlException;
            throw new SQLException("Pricing domain migration failed", exception);
        } finally {
            target.setAutoCommit(true);
        }
    }

    private static int copy(Connection source, Connection target, String table, List<String> columns)
            throws SQLException {
        var columnList = String.join(", ", columns);
        var placeholders = String.join(", ", Collections.nCopies(columns.size(), "?"));
        var selectSql = "SELECT " + columnList + " FROM " + table + " ORDER BY id";
        var insertSql = "INSERT INTO " + table + " (" + columnList + ") VALUES (" + placeholders + ")";
        int copied = 0;
        try (var select = source.prepareStatement(selectSql);
             var rows = select.executeQuery();
             var insert = target.prepareStatement(insertSql)) {
            while (rows.next()) {
                for (int index = 1; index <= columns.size(); index++) {
                    insert.setObject(index, rows.getObject(index));
                }
                insert.addBatch();
                copied++;
            }
            insert.executeBatch();
        }
        return copied;
    }

    private static void acquireTargetLock(Connection target) throws SQLException {
        try (var statement = target.prepareStatement("SELECT pg_advisory_xact_lock(?)")) {
            statement.setLong(1, 4_912_086L);
            statement.execute();
        }
    }

    private static void assertTargetIsEmpty(Connection target) throws SQLException {
        if (count(target, "pricing_plans") != 0
                || count(target, "pricing_routes") != 0
                || count(target, "pricing_brackets") != 0) {
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

    private static void assertSameIds(Connection source, Connection target, String table) throws SQLException {
        var sourceIds = ids(source, table);
        var targetIds = ids(target, table);
        if (!sourceIds.equals(targetIds)) {
            throw new SQLException("The migrated IDs do not match for " + table);
        }
    }

    private static java.util.Set<Object> ids(Connection connection, String table) throws SQLException {
        var values = new HashSet<Object>();
        try (var statement = connection.prepareStatement("SELECT id FROM " + table);
             var result = statement.executeQuery()) {
            while (result.next()) values.add(result.getObject(1));
        }
        return values;
    }

    record MigrationResult(int plans, int routes, int brackets) {}

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
