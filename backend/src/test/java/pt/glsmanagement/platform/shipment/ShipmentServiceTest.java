package pt.glsmanagement.platform.shipment;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.glsmanagement.platform.entity.RecipientRegistration;
import pt.glsmanagement.platform.entity.RecipientRegistrationService;
import pt.glsmanagement.platform.entity.RecipientResponse;
import pt.glsmanagement.platform.pricing.PricingQuote;
import pt.glsmanagement.platform.pricing.PricingQuoteService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShipmentServiceTest {
    @Mock ShipmentRepository shipments;
    @Mock CustomerShipmentLookup customers;
    @Mock RecipientRegistrationService recipients;
    @Mock PricingQuoteService pricing;
    ShipmentService service;

    private final UUID customerId = UUID.randomUUID();
    private final UUID planId = UUID.randomUUID();
    private final UUID routeId = UUID.randomUUID();
    private final UUID recipientId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ShipmentService(shipments, customers, recipients, pricing);
    }

    @Test
    void createsPricedShipmentAndRegistersRecipientInOneFlow() {
        var request = request(LocalDate.of(2026, 9, 30), PaymentStatus.PENDING);
        when(customers.customer(customerId)).thenReturn(new Shipment.CustomerSnapshot("100001", "Cliente Norte"));
        when(customers.nextShipmentNumber(request.shipmentDate())).thenReturn("LTFT-2026-000001");
        when(pricing.quote(any())).thenReturn(quote());
        when(recipients.registerFromShipment(request.recipient())).thenReturn(recipient());
        when(shipments.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(request, "operador.teste");

        assertThat(result.shipmentNumber()).isEqualTo("LTFT-2026-000001");
        assertThat(result.customerCode()).isEqualTo("100001");
        assertThat(result.recipientDesignation()).isEqualTo("Destino Lisboa");
        assertThat(result.total()).isEqualByComparingTo("6.00");
        assertThat(result.paymentStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.dueDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(result.createdBy()).isEqualTo("operador.teste");
        verify(recipients).registerFromShipment(request.recipient());
        verify(shipments).save(any(Shipment.class));
    }

    @Test
    void defaultsDueDateAndPaidTimestamp() {
        var request = request(null, PaymentStatus.PAID);
        when(customers.customer(customerId)).thenReturn(new Shipment.CustomerSnapshot("100001", "Cliente Norte"));
        when(customers.nextShipmentNumber(request.shipmentDate())).thenReturn("LTFT-2026-000002");
        when(pricing.quote(any())).thenReturn(quote());
        when(recipients.registerFromShipment(request.recipient())).thenReturn(recipient());
        when(shipments.save(any(Shipment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.create(request, " ");

        assertThat(result.dueDate()).isEqualTo(request.shipmentDate().plusDays(30));
        assertThat(result.paidAt()).isNotNull();
        assertThat(result.createdBy()).isEqualTo("Sistema");
    }

    @Test
    void rejectsDueDateBeforeShipmentWithoutWritingAnything() {
        var request = request(LocalDate.of(2026, 8, 1), PaymentStatus.PENDING);

        assertThatThrownBy(() -> service.create(request, "operator"))
                .isInstanceOf(ShipmentException.class)
                .extracting(error -> ((ShipmentException) error).reason())
                .isEqualTo(ShipmentException.Reason.INVALID_DATA);
        verifyNoInteractions(customers, recipients, pricing, shipments);
    }

    @Test
    void rejectsRecipientOutsideTheSelectedRouteCountry() {
        var request = request(null, PaymentStatus.PENDING);
        var spanishQuote = new PricingQuote(planId, "LTFT-BASE", 1, routeId, "ROTA_ES_48H", "Espanha 48h", "ES",
                new BigDecimal("4.000"), BigDecimal.ZERO, new BigDecimal("4.000"),
                new BigDecimal("6.89"), new BigDecimal("0.48"), new BigDecimal("7.37"),
                new BigDecimal("1.70"), new BigDecimal("9.07"), "EUR");
        when(customers.customer(customerId)).thenReturn(new Shipment.CustomerSnapshot("100001", "Cliente Norte"));
        when(pricing.quote(any())).thenReturn(spanishQuote);

        assertThatThrownBy(() -> service.create(request, "operator"))
                .isInstanceOf(ShipmentException.class);
        verifyNoInteractions(recipients, shipments);
    }

    @Test
    void mapsPersistedShipmentsToCustomerAccountServices() {
        var request = request(null, PaymentStatus.PENDING);
        var shipment = Shipment.create("LTFT-2026-000003", new Shipment.CustomerSnapshot("100001", "Cliente Norte"),
                recipient(), request, quote(), "operator");
        when(customers.customer(customerId)).thenReturn(new Shipment.CustomerSnapshot("100001", "Cliente Norte"));
        when(shipments.findByCustomerIdOrderByShipmentDateDescCreatedAtDesc(customerId)).thenReturn(List.of(shipment));

        var result = service.customerServices(customerId);

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.reference()).isEqualTo("LTFT-2026-000003");
            assertThat(item.description()).contains("Portugal 24h", "Destino Lisboa");
            assertThat(item.amount()).isEqualByComparingTo("6.00");
        });
    }

    private ShipmentRequest request(LocalDate dueDate, PaymentStatus paymentStatus) {
        return new ShipmentRequest(customerId, planId, "ROTA_PT_24H", LocalDate.of(2026, 8, 20), dueDate, 1,
                new BigDecimal("4.000"), null, null, null,
                new RecipientRegistration("LIS-01", "Destino Lisboa", "Ana", "Avenida da República, 45",
                        "1050-187", "Lisboa", "PT", "destino@example.test", "211000001", null),
                paymentStatus);
    }

    private PricingQuote quote() {
        return new PricingQuote(planId, "LTFT-BASE", 1, routeId, "ROTA_PT_24H", "Portugal 24h", "PT",
                new BigDecimal("4.000"), new BigDecimal("0.000"),
                new BigDecimal("4.000"), new BigDecimal("4.56"), new BigDecimal("0.32"),
                new BigDecimal("4.88"), new BigDecimal("1.12"), new BigDecimal("6.00"), "EUR");
    }

    private RecipientResponse recipient() {
        return new RecipientResponse(recipientId, "LIS-01", "Destino Lisboa", "Ana",
                "Avenida da República, 45", "1050-187", "Lisboa", "PT", "destino@example.test",
                "211000001", null, OffsetDateTime.now());
    }
}
