package pt.glsmanagement.platform.integration;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CarrierShipment(
        String carrier,
        String externalShipmentId,
        String customerReference,
        LocalDate shipmentDate,
        Recipient recipient,
        int packages,
        BigDecimal weight
) {
    public record Recipient(
            String name,
            String address,
            String postalCode,
            String city,
            String country
    ) {
    }
}

