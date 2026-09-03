package pt.glsmanagement.platform.servicecatalog;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.glsmanagement.platform.billing.BillingZoneLookup;
import pt.glsmanagement.platform.customer.CustomerReferenceLookup;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Stream;

import static pt.glsmanagement.platform.servicecatalog.OperationalServiceDtos.*;

@Service
class OperationalServiceService {
    private static final Set<String> AGENCIES = Set.of("LTFT01", "LTFT02");
    private final OperationalServiceRepository repository;
    private final ServiceGroupService groups;
    private final BillingZoneLookup billingZones;
    private final CustomerReferenceLookup customers;

    OperationalServiceService(OperationalServiceRepository repository, ServiceGroupService groups,
                              BillingZoneLookup billingZones, CustomerReferenceLookup customers) {
        this.repository = repository;
        this.groups = groups;
        this.billingZones = billingZones;
        this.customers = customers;
    }

    @Transactional(readOnly = true)
    List<Summary> list(Boolean active) {
        var values = active == null ? repository.findAllByOrderByDesignation()
                : repository.findAllByActiveOrderByDesignation(active);
        return values.stream().map(Summary::from).toList();
    }

    @Transactional(readOnly = true)
    Response detail(UUID id) { return Response.from(find(id)); }

    @Transactional
    Response create(Request request, String actor) {
        var normalized = normalize(request);
        if (repository.existsByCodeIgnoreCase(normalized.identity().code()))
            throw duplicate();
        validate(normalized, null);
        var group = groups.requireActive(normalized.identity().groupId());
        var associated = resolve(normalized.pickupRules().associatedPickupServiceId(), null);
        var intercity = resolve(normalized.pickupRules().intercityPickupServiceId(), null);
        return Response.from(repository.save(OperationalService.create(normalized, group, associated, intercity, actor)));
    }

    @Transactional
    Response update(UUID id, Request request, String actor) {
        var value = find(id);
        var normalized = normalize(request);
        if (repository.existsByCodeIgnoreCaseAndIdNot(normalized.identity().code(), id))
            throw duplicate();
        validate(normalized, id);
        var group = groups.requireActive(normalized.identity().groupId());
        var associated = resolve(normalized.pickupRules().associatedPickupServiceId(), id);
        var intercity = resolve(normalized.pickupRules().intercityPickupServiceId(), id);
        value.update(normalized, group, associated, intercity, actor);
        return Response.from(value);
    }

    @Transactional
    Response status(UUID id, boolean active, String actor) {
        var value = find(id);
        value.setActive(active, actor);
        return Response.from(value);
    }

    private void validate(Request request, UUID currentId) {
        var transit = request.transit();
        range(transit.transitMinHours(), transit.transitMaxHours());
        var zoneIds = new HashSet<UUID>();
        for (var zone : transit.zones()) {
            if (!zoneIds.add(zone.billingZoneId())) invalid();
            range(zone.transitMinHours(), zone.transitMaxHours());
        }
        if (!billingZones.allActive(zoneIds)) invalid();

        var limits = request.limits();
        range(limits.totalVolumesMin(), limits.totalVolumesMax());
        range(limits.totalWeightMinKg(), limits.totalWeightMaxKg());
        var packageTypes = new HashSet<OperationalService.PackageType>();
        for (var limit : limits.packageLimits()) {
            if (!packageTypes.add(limit.packageType())) invalid();
            if (limit.maxCombinedCm() != null && Stream.of(limit.maxLengthCm(), limit.maxWidthCm(), limit.maxHeightCm())
                    .filter(Objects::nonNull)
                    .anyMatch(dimension -> dimension.compareTo(limit.maxCombinedCm()) > 0)) invalid();
        }

        var schedule = request.schedule();
        if ((schedule.pickupStart() == null) != (schedule.pickupEnd() == null)) invalid();
        if (schedule.pickupStart() != null && !schedule.pickupEnd().isAfter(schedule.pickupStart())) invalid();
        if (request.definitions().noPickup() && (schedule.pickupStart() != null
                || schedule.minimumAdvanceMinutes() != null || !schedule.pickupDays().isEmpty()
                || request.pickupRules().associatedPickupServiceId() != null
                || request.pickupRules().intercityPickupServiceId() != null
                || request.characteristics().pickupOnly())) invalid();
        if (!AGENCIES.containsAll(request.restrictions().blockedAgencies())) invalid();
        if (!customers.allExist(request.restrictions().exclusiveCustomerIds())) invalid();
        var associated = request.pickupRules().associatedPickupServiceId();
        var intercity = request.pickupRules().intercityPickupServiceId();
        if (currentId != null && (currentId.equals(associated) || currentId.equals(intercity))) invalid();
    }

    private OperationalService resolve(UUID id, UUID currentId) {
        if (id == null) return null;
        if (id.equals(currentId)) invalid();
        var value = find(id);
        if (!value.active()) invalid();
        return value;
    }

    private OperationalService find(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new OperationalCatalogException(OperationalCatalogException.Reason.NOT_FOUND));
    }

    private static Request normalize(Request request) {
        var i = request.identity();
        var identity = new IdentityRequest(code(i.code()), text(i.designation()), code(i.groupId()),
                i.transportType(), i.active());
        var p = request.pricing();
        var pricing = new PricingRequest(p.priceCalculationRule(), p.salesCalculationRule(), nullableCode(p.forcedCarrier()),
                p.vatMode(), p.pricePerVolume(), p.pricePerCubicMeter(), p.pricePerDimensions(), p.priceByBracket());
        var d = request.definitions();
        var definitions = new DefinitionsRequest(d.requiresEmail(), d.autoSubmitWebservice(), d.requiresKilometres(),
                d.forceReturn(), d.noPickup(), d.requiresDimensions(), d.insuredValue(), nullableText(d.mapIdentifier()));
        var r = request.restrictions();
        var agencies = new TreeSet<String>();
        r.blockedAgencies().forEach(value -> agencies.add(code(value)));
        var restrictions = new RestrictionsRequest(Set.copyOf(r.exclusiveCustomerIds()), Set.copyOf(agencies));
        return new Request(identity, request.transit(), pricing, request.limits(), request.schedule(),
                request.pickupRules(), request.characteristics(), request.additionalServices(), definitions, restrictions);
    }

    private static void range(Integer min, Integer max) { if (min != null && max != null && max < min) invalid(); }
    private static void range(BigDecimal min, BigDecimal max) { if (min != null && max != null && max.compareTo(min) < 0) invalid(); }
    private static void invalid() { throw new OperationalCatalogException(OperationalCatalogException.Reason.INVALID_CONFIGURATION); }
    private static OperationalCatalogException duplicate() {
        return new OperationalCatalogException(OperationalCatalogException.Reason.DUPLICATE);
    }
    private static String code(String value) { return value.trim().toUpperCase(Locale.ROOT); }
    private static String text(String value) { return value.trim().replaceAll("\\s+", " "); }
    private static String nullableText(String value) { return value == null || value.isBlank() ? null : text(value); }
    private static String nullableCode(String value) { return value == null || value.isBlank() ? null : code(value); }
}

