package pt.glsmanagement.platform.shipment;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;

public record ShipmentResponse(
        UUID id,
        String shipmentNumber,
        String externalReference,
        UUID customerId,
        String customerCode,
        String customerName,
        UUID recipientId,
        String recipientCode,
        String recipientDesignation,
        String recipientAddress,
        String recipientPostalCode,
        String recipientLocality,
        String recipientCountry,
        UUID pricingPlanId,
        String pricingPlanCode,
        int pricingPlanVersion,
        String routeCode,
        String routeDesignation,
        String serviceCode,
        LocalDate shipmentDate,
        LocalDate dueDate,
        int parcelCount,
        BigDecimal actualWeightKg,
        BigDecimal volumetricWeightKg,
        BigDecimal chargeableWeightKg,
        BigDecimal basePrice,
        BigDecimal fuelSurcharge,
        BigDecimal subtotal,
        BigDecimal vat,
        BigDecimal total,
        String currency,
        ShipmentStatus shipmentStatus,
        PaymentStatus paymentStatus,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt,
        String createdBy
) {
    static ShipmentResponse from(Shipment shipment) {
        return new ShipmentResponse(shipment.id(), shipment.shipmentNumber(), shipment.externalReference(),
                shipment.customerId(), shipment.customerCode(), shipment.customerName(), shipment.recipientId(),
                shipment.recipientCode(), shipment.recipientDesignation(), shipment.recipientAddress(),
                shipment.recipientPostalCode(), shipment.recipientLocality(), shipment.recipientCountry(),
                shipment.pricingPlanId(), shipment.pricingPlanCode(), shipment.pricingPlanVersion(),
                shipment.routeCode(), shipment.routeDesignation(), shipment.serviceCode(), shipment.shipmentDate(),
                shipment.dueDate(), shipment.parcelCount(), shipment.actualWeightKg(),
                shipment.volumetricWeightKg(), shipment.chargeableWeightKg(), shipment.basePrice(),
                shipment.fuelSurcharge(), shipment.subtotal(), shipment.vat(), shipment.total(), shipment.currency(),
                shipment.shipmentStatus(), shipment.paymentStatus(), shipment.paidAt(), shipment.createdAt(),
                shipment.createdBy());
    }
}
