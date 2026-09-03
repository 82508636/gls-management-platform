package pt.glsmanagement.platform.pricing;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.util.*;

import static pt.glsmanagement.platform.pricing.PricingDtos.*;

@Service
class PricingService implements PricingQuoteService {
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final BigDecimal CUBIC_CENTIMETRES_PER_CUBIC_METRE = new BigDecimal("1000000");
    private final PricingPlanRepository plans;
    private final PricingRouteRepository routes;

    PricingService(PricingPlanRepository plans, PricingRouteRepository routes) {
        this.plans = plans;
        this.routes = routes;
    }

    @Transactional(readOnly = true)
    List<PlanSummary> list() { return plans.findAllByOrderByUpdatedAtDesc().stream().map(PlanSummary::from).toList(); }

    @Transactional(readOnly = true)
    PlanDetail detail(UUID id) { return PlanDetail.from(findPlan(id)); }

    @Transactional
    PlanDetail create(PlanRequest request, String actor) {
        validateValidity(request.validFrom(), request.validTo());
        var code = normalizeCode(request.code());
        if (plans.existsByCodeIgnoreCaseAndVersion(code, request.version())) throw PricingException.duplicate();
        var plan = PricingPlan.create(code, normalizeText(request.designation()), request.version(), request.validFrom(),
                request.validTo(), normalizeCurrency(request.currency()), moneyPercent(request.fuelSurchargePercent()),
                moneyPercent(request.vatPercent()), actor);
        return PlanDetail.from(plans.save(plan));
    }

    @Transactional
    PlanDetail update(UUID id, PlanUpdateRequest request, String actor) {
        validateValidity(request.validFrom(), request.validTo());
        var plan = findPlan(id);
        plan.update(normalizeText(request.designation()), request.validFrom(), request.validTo(),
                normalizeCurrency(request.currency()), moneyPercent(request.fuelSurchargePercent()),
                moneyPercent(request.vatPercent()), actor);
        return PlanDetail.from(plan);
    }

    @Transactional
    RouteResponse addRoute(UUID planId, RouteCreateRequest request, String actor) {
        var plan = findPlan(planId);
        plan.requireDraft();
        var code = normalizeCode(request.code());
        if (routes.existsByPlanIdAndCodeIgnoreCase(planId, code)) throw PricingException.duplicate();
        validateRoute(request.maxPieceWeightKg(), request.additionalStepPrice(), request.brackets());
        var route = PricingRoute.create(plan, code, normalizeText(request.designation()),
                normalizeCountry(request.destinationCountry()), normalizeText(request.deliveryCommitment()),
                request.volumetricFactor(), request.maxPieceWeightKg(), request.maxCombinedDimensionsCm(),
                request.additionalStepKg(), request.additionalStepPrice(), request.enabled(), request.sortOrder());
        route.replaceBrackets(sortedBrackets(request.brackets()));
        plan.addRoute(route, actor);
        return RouteResponse.from(route);
    }

    @Transactional
    RouteResponse updateRoute(UUID planId, UUID routeId, RouteUpdateRequest request, String actor) {
        var route = routes.findByIdAndPlanId(routeId, planId).orElseThrow(PricingException::notFound);
        var code = normalizeCode(request.code());
        routes.findByPlanIdAndCodeIgnoreCase(planId, code)
                .filter(existing -> !existing.id().equals(routeId))
                .ifPresent(existing -> { throw PricingException.duplicate(); });
        validateRoute(request.maxPieceWeightKg(), request.additionalStepPrice(), request.brackets());
        route.update(code, normalizeText(request.designation()), normalizeCountry(request.destinationCountry()),
                normalizeText(request.deliveryCommitment()), request.volumetricFactor(), request.maxPieceWeightKg(),
                request.maxCombinedDimensionsCm(), request.additionalStepKg(), request.additionalStepPrice(),
                request.enabled(), request.sortOrder());
        route.clearBrackets();
        routes.flush();
        route.replaceBrackets(sortedBrackets(request.brackets()));
        route.plan().touch(actor);
        return RouteResponse.from(route);
    }

    @Transactional
    void deleteRoute(UUID planId, UUID routeId, String actor) {
        var route = routes.findByIdAndPlanId(routeId, planId).orElseThrow(PricingException::notFound);
        var plan = route.plan();
        plan.requireDraft();
        plan.routes().remove(route);
        plan.touch(actor);
    }

    @Transactional
    PlanDetail setStatus(UUID id, StatusRequest request, String actor) {
        var plan = findPlan(id);
        if (request.status() == PricingPlan.Status.ACTIVE) {
            validateActivation(plan);
            plan.activate(actor);
        } else if (request.status() == PricingPlan.Status.ARCHIVED) {
            plan.archive(actor);
        } else {
            throw PricingException.invalidState();
        }
        return PlanDetail.from(plan);
    }

    @Transactional(readOnly = true)
    SimulationResponse simulate(SimulationRequest request) {
        var plan = findPlan(request.planId());
        var route = routes.findByPlanIdAndCodeIgnoreCase(plan.id(), normalizeCode(request.routeCode()))
                .orElseThrow(PricingException::notFound);
        if (!route.enabled()) throw PricingException.invalidConfiguration();
        var parcels = request.parcelCount() == 0 ? 1 : request.parcelCount();
        var weightPerParcel = request.actualWeightKg().divide(BigDecimal.valueOf(parcels), 3, RoundingMode.CEILING);
        if (weightPerParcel.compareTo(route.maxPieceWeightKg()) > 0) throw PricingException.outOfRange();
        var volumetric = volumetricWeight(request, route.volumetricFactor(), parcels);
        var chargeable = request.actualWeightKg().max(volumetric).setScale(3, RoundingMode.CEILING);
        var brackets = route.brackets().stream().sorted(Comparator.comparing(PricingBracket::upToWeightKg)).toList();
        if (brackets.isEmpty()) throw PricingException.invalidConfiguration();
        var selected = brackets.stream().filter(item -> chargeable.compareTo(item.upToWeightKg()) <= 0).findFirst().orElse(brackets.getLast());
        var basePrice = selected.price();
        var extraSteps = 0;
        if (chargeable.compareTo(selected.upToWeightKg()) > 0) {
            if (route.additionalStepPrice() == null) throw PricingException.outOfRange();
            extraSteps = chargeable.subtract(selected.upToWeightKg())
                    .divide(route.additionalStepKg(), 0, RoundingMode.CEILING).intValueExact();
            basePrice = basePrice.add(route.additionalStepPrice().multiply(BigDecimal.valueOf(extraSteps)));
        }
        basePrice = money(basePrice);
        var fuel = percentage(basePrice, plan.fuelSurchargePercent());
        var subtotal = money(basePrice.add(fuel));
        var vat = percentage(subtotal, plan.vatPercent());
        var total = money(subtotal.add(vat));
        return new SimulationResponse(plan.id(), plan.code(), plan.version(), route.code(), route.designation(),
                request.actualWeightKg(), volumetric, chargeable, selected.upToWeightKg(), extraSteps,
                basePrice, fuel, subtotal, vat, total, plan.currency());
    }

    @Override
    @Transactional(readOnly = true)
    public PricingQuote quote(PricingQuoteRequest request) {
        var simulation = simulate(new SimulationRequest(request.planId(), request.routeCode(),
                request.actualWeightKg(), request.parcelCount(), request.lengthCm(), request.widthCm(),
                request.heightCm()));
        var route = routes.findByPlanIdAndCodeIgnoreCase(request.planId(), normalizeCode(request.routeCode()))
                .orElseThrow(PricingException::notFound);
        return new PricingQuote(simulation.planId(), simulation.planCode(), simulation.planVersion(), route.id(),
                simulation.routeCode(), simulation.routeDesignation(), route.destinationCountry(),
                simulation.actualWeightKg(), simulation.volumetricWeightKg(), simulation.chargeableWeightKg(),
                simulation.basePrice(), simulation.fuelSurcharge(), simulation.subtotal(), simulation.vat(),
                simulation.total(), simulation.currency());
    }

    private PricingPlan findPlan(UUID id) { return plans.findById(id).orElseThrow(PricingException::notFound); }

    private static BigDecimal volumetricWeight(SimulationRequest request, BigDecimal factor, int parcels) {
        var supplied = 0;
        if (request.lengthCm() != null) supplied++;
        if (request.widthCm() != null) supplied++;
        if (request.heightCm() != null) supplied++;
        if (supplied == 0) return BigDecimal.ZERO.setScale(3);
        if (supplied != 3) throw PricingException.invalidConfiguration();
        return request.lengthCm().multiply(request.widthCm()).multiply(request.heightCm()).multiply(factor)
                .multiply(BigDecimal.valueOf(parcels)).divide(CUBIC_CENTIMETRES_PER_CUBIC_METRE, 3, RoundingMode.CEILING);
    }

    private static void validateActivation(PricingPlan plan) {
        var valid = plan.routes().stream().anyMatch(route -> route.enabled() && !route.brackets().isEmpty());
        if (!valid) throw PricingException.invalidConfiguration();
    }

    private static void validateRoute(BigDecimal maxWeight, BigDecimal additionalPrice, List<BracketRequest> brackets) {
        if (brackets == null || brackets.isEmpty()) throw PricingException.invalidConfiguration();
        var sorted = sortedBrackets(brackets);
        BigDecimal previous = BigDecimal.ZERO;
        for (var bracket : sorted) {
            if (bracket.upToWeightKg().compareTo(previous) <= 0 || bracket.upToWeightKg().compareTo(maxWeight) > 0)
                throw PricingException.invalidConfiguration();
            previous = bracket.upToWeightKg();
        }
        if (previous.compareTo(maxWeight) < 0 && additionalPrice == null) throw PricingException.invalidConfiguration();
    }

    private static List<BracketRequest> sortedBrackets(List<BracketRequest> values) {
        return values.stream().sorted(Comparator.comparing(BracketRequest::upToWeightKg)).toList();
    }
    private static void validateValidity(java.time.LocalDate from, java.time.LocalDate to) {
        if (to != null && to.isBefore(from)) throw PricingException.invalidConfiguration();
    }
    private static String normalizeCode(String value) { return value.trim().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_-]", "_"); }
    private static String normalizeText(String value) { return value.trim().replaceAll("\\s+", " "); }
    private static String normalizeCurrency(String value) { return value.trim().toUpperCase(Locale.ROOT); }
    private static String normalizeCountry(String value) { return value.trim().toUpperCase(Locale.ROOT); }
    private static BigDecimal moneyPercent(BigDecimal value) { return value.setScale(4, RoundingMode.HALF_UP); }
    private static BigDecimal percentage(BigDecimal value, BigDecimal percent) { return money(value.multiply(percent).divide(HUNDRED, 6, RoundingMode.HALF_UP)); }
    private static BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
}
