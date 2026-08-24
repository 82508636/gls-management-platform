package pt.glsmanagement.platform.integration.gls;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockGlsCarrierProviderTest {
    private final MockGlsCarrierProvider provider = new MockGlsCarrierProvider();

    @Test
    void returnsMockShipmentsIncludingAnUnmatchedReference() {
        assertThat(provider.getShipments())
                .hasSize(3)
                .anySatisfy(shipment -> assertThat(shipment.customerReference()).isNull());
    }

    @Test
    void findsShipmentByExternalId() {
        assertThat(provider.getShipment("MOCK-001"))
                .isPresent()
                .get()
                .extracting("carrier")
                .isEqualTo("GLS");
    }
}

