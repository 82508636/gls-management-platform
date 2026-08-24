package pt.glsmanagement.platform.integration.gls;

import org.springframework.stereotype.Component;
import pt.glsmanagement.platform.integration.CarrierProvider;
import pt.glsmanagement.platform.integration.CarrierShipment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Component
public class MockGlsCarrierProvider implements CarrierProvider {
    private final List<CarrierShipment> shipments = List.of(
            shipment("MOCK-001", "CUSTOMER-A", "Ana Martins", "Porto", "4000-100", "2.4"),
            shipment("MOCK-002", "CUSTOMER-B", "Bruno Costa", "Lisboa", "1000-001", "7.8"),
            shipment("MOCK-003", null, "Carla Sousa", "Coimbra", "3000-200", "1.2")
    );

    @Override
    public List<CarrierShipment> getShipments() {
        return shipments;
    }

    @Override
    public Optional<CarrierShipment> getShipment(String externalId) {
        return shipments.stream()
                .filter(shipment -> shipment.externalShipmentId().equals(externalId))
                .findFirst();
    }

    private static CarrierShipment shipment(
            String id, String customerReference, String recipientName, String city, String postalCode, String weight
    ) {
        return new CarrierShipment(
                "GLS",
                id,
                customerReference,
                LocalDate.of(2026, 8, 12),
                new CarrierShipment.Recipient(recipientName, "Morada de teste", postalCode, city, "PT"),
                1,
                new BigDecimal(weight)
        );
    }
}

