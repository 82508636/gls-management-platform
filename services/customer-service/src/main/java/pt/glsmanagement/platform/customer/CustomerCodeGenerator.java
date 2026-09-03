package pt.glsmanagement.platform.customer;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
class CustomerCodeGenerator {
    private final JdbcTemplate jdbc;

    CustomerCodeGenerator(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    String next(String agency) {
        var prefix = switch (agency) {
            case "LTFT01" -> "1";
            case "LTFT02" -> "2";
            default -> throw new IllegalArgumentException("Unsupported agency");
        };
        var value = jdbc.queryForObject("""
                UPDATE customer_code_counters
                SET next_number = next_number + 1
                WHERE agency = ? AND next_number <= 99999
                RETURNING next_number - 1
                """, Integer.class, agency);
        if (value == null || value > 99_999) throw new IllegalStateException("Customer code range exhausted");
        return prefix + String.format("%05d", value);
    }
}
