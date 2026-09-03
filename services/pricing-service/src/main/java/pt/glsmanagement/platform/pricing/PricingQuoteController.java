package pt.glsmanagement.platform.pricing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/internal/v1/pricing")
class PricingQuoteController {
    private final PricingQuoteService service;

    PricingQuoteController(PricingQuoteService service) { this.service = service; }

    @PostMapping("/quotes")
    PricingQuote quote(@Valid @RequestBody QuoteRequest request) {
        return service.quote(new PricingQuoteRequest(request.planId(), request.routeCode(), request.actualWeightKg(),
                request.parcelCount(), request.lengthCm(), request.widthCm(), request.heightCm()));
    }

    record QuoteRequest(
            @NotNull UUID planId,
            @NotBlank String routeCode,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal actualWeightKg,
            @Min(1) int parcelCount,
            @DecimalMin(value = "0", inclusive = false) BigDecimal lengthCm,
            @DecimalMin(value = "0", inclusive = false) BigDecimal widthCm,
            @DecimalMin(value = "0", inclusive = false) BigDecimal heightCm) {}
}
