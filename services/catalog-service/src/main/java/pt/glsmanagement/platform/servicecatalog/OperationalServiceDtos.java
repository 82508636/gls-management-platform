package pt.glsmanagement.platform.servicecatalog;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

final class OperationalServiceDtos {
    private OperationalServiceDtos() {}

    record IdentityRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z0-9_-]{1,40}") String code,
            @NotBlank @Size(max = 200) String designation,
            @NotBlank @Size(max = 40) String groupId,
            @NotNull OperationalService.TransportType transportType,
            @NotNull Boolean active) {}

    record ZoneRuleRequest(
            @NotNull UUID billingZoneId,
            @Min(0) Integer transitMinHours,
            @Min(0) Integer transitMaxHours) {}

    record TransitRequest(
            @Min(0) Integer transitMinHours,
            @Min(0) Integer transitMaxHours,
            LocalTime deliveryCutoff,
            @Min(1) @Max(5) int urgency,
            @NotNull List<@Valid ZoneRuleRequest> zones) {}

    record PricingRequest(
            @NotNull OperationalService.PriceCalculationRule priceCalculationRule,
            @NotNull OperationalService.SalesCalculationRule salesCalculationRule,
            @Size(max = 40) String forcedCarrier,
            @NotNull OperationalService.VatMode vatMode,
            boolean pricePerVolume,
            boolean pricePerCubicMeter,
            boolean pricePerDimensions,
            boolean priceByBracket) {}

    record PackageLimitRequest(
            @NotNull OperationalService.PackageType packageType,
            @DecimalMin(value = "0", inclusive = false) BigDecimal maxWeightKg,
            @DecimalMin(value = "0", inclusive = false) BigDecimal maxLengthCm,
            @DecimalMin(value = "0", inclusive = false) BigDecimal maxWidthCm,
            @DecimalMin(value = "0", inclusive = false) BigDecimal maxHeightCm,
            @DecimalMin(value = "0", inclusive = false) BigDecimal maxCombinedCm) {}

    record LimitsRequest(
            @NotNull List<@Valid PackageLimitRequest> packageLimits,
            @Min(0) Integer totalVolumesMin,
            @Min(0) Integer totalVolumesMax,
            @DecimalMin("0") BigDecimal totalWeightMinKg,
            @DecimalMin("0") BigDecimal totalWeightMaxKg) {}

    record ScheduleRequest(
            LocalTime pickupStart,
            LocalTime pickupEnd,
            @Min(0) Integer minimumAdvanceMinutes,
            @NotNull Set<DayOfWeek> pickupDays,
            @NotNull Set<DayOfWeek> deliveryDays) {}

    record PickupRulesRequest(UUID associatedPickupServiceId, UUID intercityPickupServiceId) {}

    record CharacteristicsRequest(
            boolean pickupOnly, boolean mailVatZero, boolean international, boolean seaTransport,
            boolean airTransport, boolean courier, boolean forceImport, boolean forceExport) {}

    record AdditionalServicesRequest(
            boolean allowsCod, boolean allowsReturn, boolean allowsPudo, boolean requiresDeliveryPin) {}

    record DefinitionsRequest(
            boolean requiresEmail, boolean autoSubmitWebservice, boolean requiresKilometres,
            boolean forceReturn, boolean noPickup, boolean requiresDimensions, boolean insuredValue,
            @Size(max = 120) String mapIdentifier) {}

    record RestrictionsRequest(
            @NotNull Set<UUID> exclusiveCustomerIds,
            @NotNull Set<@Pattern(regexp = "LTFT0[12]") String> blockedAgencies) {}

    record Request(
            @NotNull @Valid IdentityRequest identity,
            @NotNull @Valid TransitRequest transit,
            @NotNull @Valid PricingRequest pricing,
            @NotNull @Valid LimitsRequest limits,
            @NotNull @Valid ScheduleRequest schedule,
            @NotNull @Valid PickupRulesRequest pickupRules,
            @NotNull @Valid CharacteristicsRequest characteristics,
            @NotNull @Valid AdditionalServicesRequest additionalServices,
            @NotNull @Valid DefinitionsRequest definitions,
            @NotNull @Valid RestrictionsRequest restrictions) {}

    record StatusRequest(@NotNull Boolean active) {}

    record IdentityResponse(String code, String designation, String groupId, String groupDesignation,
                            OperationalService.TransportType transportType, boolean active) {}
    record ZoneRuleResponse(UUID billingZoneId, Integer transitMinHours, Integer transitMaxHours) {}
    record TransitResponse(Integer transitMinHours, Integer transitMaxHours, LocalTime deliveryCutoff, int urgency,
                           List<ZoneRuleResponse> zones) {}
    record PackageLimitResponse(OperationalService.PackageType packageType, BigDecimal maxWeightKg,
                                BigDecimal maxLengthCm, BigDecimal maxWidthCm, BigDecimal maxHeightCm,
                                BigDecimal maxCombinedCm) {}
    record LimitsResponse(List<PackageLimitResponse> packageLimits, Integer totalVolumesMin, Integer totalVolumesMax,
                          BigDecimal totalWeightMinKg, BigDecimal totalWeightMaxKg) {}
    record ScheduleResponse(LocalTime pickupStart, LocalTime pickupEnd, Integer minimumAdvanceMinutes,
                            Set<DayOfWeek> pickupDays, Set<DayOfWeek> deliveryDays) {}

    record Response(
            UUID id,
            IdentityResponse identity,
            TransitResponse transit,
            PricingRequest pricing,
            LimitsResponse limits,
            ScheduleResponse schedule,
            PickupRulesRequest pickupRules,
            CharacteristicsRequest characteristics,
            AdditionalServicesRequest additionalServices,
            DefinitionsRequest definitions,
            RestrictionsRequest restrictions,
            OffsetDateTime createdAt, String createdBy, OffsetDateTime updatedAt, String updatedBy, long version) {
        static Response from(OperationalService value) {
            var identity = new IdentityResponse(value.code(), value.designation(), value.group().id(),
                    value.group().designation(), value.transportType(), value.active());
            var transit = new TransitResponse(value.transitMinHours(), value.transitMaxHours(), value.deliveryCutoff(),
                    value.urgency(), value.zones().stream().map(zone -> new ZoneRuleResponse(zone.billingZoneId(),
                            zone.transitMinHours(), zone.transitMaxHours())).toList());
            var pricing = new PricingRequest(value.priceCalculationRule(), value.salesCalculationRule(),
                    value.forcedCarrier(), value.vatMode(), value.pricePerVolume(), value.pricePerCubicMeter(),
                    value.pricePerDimensions(), value.priceByBracket());
            var limits = new LimitsResponse(value.packageLimits().stream().map(item -> new PackageLimitResponse(
                    item.packageType(), item.maxWeightKg(), item.maxLengthCm(), item.maxWidthCm(), item.maxHeightCm(),
                    item.maxCombinedCm())).toList(), value.totalVolumesMin(), value.totalVolumesMax(),
                    value.totalWeightMinKg(), value.totalWeightMaxKg());
            var schedule = new ScheduleResponse(value.pickupStart(), value.pickupEnd(), value.minimumAdvanceMinutes(),
                    value.pickupDays(), value.deliveryDays());
            var pickup = new PickupRulesRequest(value.associatedPickupServiceId(), value.intercityPickupServiceId());
            var characteristics = new CharacteristicsRequest(value.pickupOnly(), value.mailVatZero(),
                    value.international(), value.seaTransport(), value.airTransport(), value.courier(),
                    value.forceImport(), value.forceExport());
            var additional = new AdditionalServicesRequest(value.allowsCod(), value.allowsReturn(), value.allowsPudo(),
                    value.requiresDeliveryPin());
            var definitions = new DefinitionsRequest(value.requiresEmail(), value.autoSubmitWebservice(),
                    value.requiresKilometres(), value.forceReturn(), value.noPickup(), value.requiresDimensions(),
                    value.insuredValue(), value.mapIdentifier());
            var restrictions = new RestrictionsRequest(value.exclusiveCustomerIds(), value.blockedAgencies());
            return new Response(value.id(), identity, transit, pricing, limits, schedule, pickup, characteristics,
                    additional, definitions, restrictions, value.createdAt(), value.createdBy(), value.updatedAt(),
                    value.updatedBy(), value.version());
        }
    }

    record Summary(UUID id, String code, String designation, String groupId, String groupDesignation,
                   OperationalService.TransportType transportType, boolean active, int urgency,
                   int zoneCount, OffsetDateTime updatedAt, String updatedBy) {
        static Summary from(OperationalService value) {
            return new Summary(value.id(), value.code(), value.designation(), value.group().id(),
                    value.group().designation(), value.transportType(), value.active(), value.urgency(),
                    value.zones().size(), value.updatedAt(), value.updatedBy());
        }
    }
}

