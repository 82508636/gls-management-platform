package pt.glsmanagement.platform.pricing;

import java.math.BigDecimal;
import java.util.UUID;

public record PricingQuote(
        UUID planId,
        String planCode,
        int planVersion,
        UUID routeId,
        String routeCode,
        String routeDesignation,
        String destinationCountry,
        BigDecimal actualWeightKg,
        BigDecimal volumetricWeightKg,
        BigDecimal chargeableWeightKg,
        BigDecimal basePrice,
        BigDecimal fuelSurcharge,
        BigDecimal subtotal,
        BigDecimal vat,
        BigDecimal total,
        String currency
) {
}

