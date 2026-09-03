package pt.glsmanagement.platform.pricing;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

final class PricingDtos {
    private PricingDtos() {}

    record PlanRequest(
            @NotBlank @Size(max = 40) String code,
            @NotBlank @Size(max = 160) String designation,
            @Min(1) int version,
            @NotNull LocalDate validFrom,
            LocalDate validTo,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal fuelSurchargePercent,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal vatPercent) {}

    record PlanUpdateRequest(
            @NotBlank @Size(max = 160) String designation,
            @NotNull LocalDate validFrom,
            LocalDate validTo,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}") String currency,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal fuelSurchargePercent,
            @NotNull @DecimalMin("0") @DecimalMax("100") BigDecimal vatPercent) {}

    record BracketRequest(
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal upToWeightKg,
            @NotNull @DecimalMin("0") BigDecimal price) {}

    record RouteCreateRequest(
            @NotBlank @Size(max = 60) String code,
            @NotBlank @Size(max = 160) String designation,
            @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String destinationCountry,
            @NotBlank @Size(max = 40) String deliveryCommitment,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal volumetricFactor,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal maxPieceWeightKg,
            @DecimalMin(value = "0", inclusive = false) BigDecimal maxCombinedDimensionsCm,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal additionalStepKg,
            @DecimalMin("0") BigDecimal additionalStepPrice,
            boolean enabled,
            @Min(0) int sortOrder,
            @NotEmpty List<@Valid BracketRequest> brackets) {}

    record RouteUpdateRequest(
            @NotBlank @Size(max = 60) String code,
            @NotBlank @Size(max = 160) String designation,
            @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String destinationCountry,
            @NotBlank @Size(max = 40) String deliveryCommitment,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal volumetricFactor,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal maxPieceWeightKg,
            @DecimalMin(value = "0", inclusive = false) BigDecimal maxCombinedDimensionsCm,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal additionalStepKg,
            @DecimalMin("0") BigDecimal additionalStepPrice,
            boolean enabled,
            @Min(0) int sortOrder,
            @NotEmpty List<@Valid BracketRequest> brackets) {}

    record StatusRequest(@NotNull PricingPlan.Status status) {}

    record PlanSummary(
            UUID id, String code, String designation, int version, LocalDate validFrom, LocalDate validTo,
            String currency, BigDecimal fuelSurchargePercent, BigDecimal vatPercent, PricingPlan.Status status,
            int routeCount, OffsetDateTime updatedAt, String updatedBy) {
        static PlanSummary from(PricingPlan plan) {
            return new PlanSummary(plan.id(), plan.code(), plan.designation(), plan.version(), plan.validFrom(),
                    plan.validTo(), plan.currency(), plan.fuelSurchargePercent(), plan.vatPercent(), plan.status(),
                    plan.routes().size(), plan.updatedAt(), plan.updatedBy());
        }
    }

    record BracketResponse(BigDecimal upToWeightKg, BigDecimal price) {
        static BracketResponse from(PricingBracket bracket) { return new BracketResponse(bracket.upToWeightKg(), bracket.price()); }
    }

    record RouteResponse(
            UUID id, String code, String designation, String destinationCountry,
            String deliveryCommitment, BigDecimal volumetricFactor, BigDecimal maxPieceWeightKg,
            BigDecimal maxCombinedDimensionsCm, BigDecimal additionalStepKg, BigDecimal additionalStepPrice,
            boolean enabled, int sortOrder, List<BracketResponse> brackets) {
        static RouteResponse from(PricingRoute route) {
            return new RouteResponse(route.id(), route.code(), route.designation(),
                    route.destinationCountry(), route.deliveryCommitment(), route.volumetricFactor(),
                    route.maxPieceWeightKg(), route.maxCombinedDimensionsCm(), route.additionalStepKg(),
                    route.additionalStepPrice(), route.enabled(), route.sortOrder(),
                    route.brackets().stream().map(BracketResponse::from).toList());
        }
    }

    record PlanDetail(
            UUID id, String code, String designation, int version, LocalDate validFrom, LocalDate validTo,
            String currency, BigDecimal fuelSurchargePercent, BigDecimal vatPercent, PricingPlan.Status status,
            OffsetDateTime createdAt, String createdBy, OffsetDateTime updatedAt, String updatedBy,
            List<RouteResponse> routes) {
        static PlanDetail from(PricingPlan plan) {
            return new PlanDetail(plan.id(), plan.code(), plan.designation(), plan.version(), plan.validFrom(),
                    plan.validTo(), plan.currency(), plan.fuelSurchargePercent(), plan.vatPercent(), plan.status(),
                    plan.createdAt(), plan.createdBy(), plan.updatedAt(), plan.updatedBy(),
                    plan.routes().stream().map(RouteResponse::from).toList());
        }
    }

    record SimulationRequest(
            @NotNull UUID planId,
            @NotBlank String routeCode,
            @NotNull @DecimalMin(value = "0", inclusive = false) BigDecimal actualWeightKg,
            @Min(1) int parcelCount,
            @DecimalMin(value = "0", inclusive = false) BigDecimal lengthCm,
            @DecimalMin(value = "0", inclusive = false) BigDecimal widthCm,
            @DecimalMin(value = "0", inclusive = false) BigDecimal heightCm) {}

    record SimulationResponse(
            UUID planId, String planCode, int planVersion, String routeCode, String routeDesignation,
            BigDecimal actualWeightKg, BigDecimal volumetricWeightKg, BigDecimal chargeableWeightKg,
            BigDecimal bracketWeightKg, int additionalSteps, BigDecimal basePrice,
            BigDecimal fuelSurcharge, BigDecimal subtotal, BigDecimal vat, BigDecimal total, String currency) {}
}
