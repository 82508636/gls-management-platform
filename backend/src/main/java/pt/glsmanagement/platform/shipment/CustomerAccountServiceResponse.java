package pt.glsmanagement.platform.shipment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CustomerAccountServiceResponse(
        UUID id,
        String reference,
        String description,
        LocalDate serviceDate,
        LocalDate dueDate,
        BigDecimal amount,
        PaymentStatus status,
        LocalDate paidAt
) {
    static CustomerAccountServiceResponse from(Shipment shipment) {
        return new CustomerAccountServiceResponse(shipment.id(), shipment.shipmentNumber(),
                shipment.routeDesignation() + " · " + shipment.recipientDesignation(), shipment.shipmentDate(),
                shipment.dueDate(), shipment.total(), shipment.paymentStatus(),
                shipment.paidAt() == null ? null : shipment.paidAt().toLocalDate());
    }
}
