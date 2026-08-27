package pt.glsmanagement.platform.shipment;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.UUID;

@Component
class CustomerShipmentLookup {
    private final JdbcClient jdbc;

    CustomerShipmentLookup(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    Shipment.CustomerSnapshot customer(UUID id) {
        return jdbc.sql("select customer_code, shipping_name from customers where id = :id")
                .param("id", id)
                .query((result, row) -> new Shipment.CustomerSnapshot(
                        result.getString("customer_code"), result.getString("shipping_name")))
                .optional()
                .orElseThrow(ShipmentException::notFound);
    }

    String nextShipmentNumber(LocalDate shipmentDate) {
        var sequence = jdbc.sql("select nextval('shipment_number_seq')").query(Long.class).single();
        return "LTFT-%d-%06d".formatted(shipmentDate.getYear(), sequence);
    }
}
