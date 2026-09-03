package pt.glsmanagement.platform.pricing;

import java.math.BigDecimal;
import java.util.UUID;

public record PricingQuoteRequest(
        UUID planId,
        String routeCode,
        BigDecimal actualWeightKg,
        int parcelCount,
        BigDecimal lengthCm,
        BigDecimal widthCm,
        BigDecimal heightCm
) {
}

