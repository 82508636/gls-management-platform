package pt.glsmanagement.platform.shipment;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import pt.glsmanagement.platform.entity.RecipientRegistration;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ShipmentRequest(
        @NotNull UUID customerId,
        @NotNull UUID pricingPlanId,
        @NotBlank @Size(max = 60) String routeCode,
        @NotNull LocalDate shipmentDate,
        LocalDate dueDate,
        @Min(1) int parcelCount,
        @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal actualWeightKg,
        @DecimalMin(value = "0", inclusive = false) BigDecimal lengthCm,
        @DecimalMin(value = "0", inclusive = false) BigDecimal widthCm,
        @DecimalMin(value = "0", inclusive = false) BigDecimal heightCm,
        @NotNull @Valid RecipientRegistration recipient,
        @NotNull PaymentStatus paymentStatus
) {
}
