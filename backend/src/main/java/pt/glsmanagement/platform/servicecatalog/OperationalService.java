package pt.glsmanagement.platform.servicecatalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "operational_services")
class OperationalService {
    enum TransportType { LARGE_VOLUMES, SMALL_VOLUMES }
    enum PriceCalculationRule { WEIGHT }
    enum SalesCalculationRule { DEFAULT }
    enum VatMode { AUTO, STANDARD, ZERO, EXEMPT }
    enum PackageType { BOX, DOCUMENT, PALLET }

    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 40) private String code;
    @Column(nullable = false, length = 200) private String designation;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "group_id") private ServiceGroup group;
    @Enumerated(EnumType.STRING) @Column(name = "transport_type", nullable = false, length = 30) private TransportType transportType;
    @Column(nullable = false) private boolean active;

    @Column(name = "transit_min_hours") private Integer transitMinHours;
    @Column(name = "transit_max_hours") private Integer transitMaxHours;
    @Column(name = "delivery_cutoff") private LocalTime deliveryCutoff;
    @Column(nullable = false) private int urgency;

    @Enumerated(EnumType.STRING) @Column(name = "price_calculation_rule", nullable = false, length = 30)
    private PriceCalculationRule priceCalculationRule;
    @Enumerated(EnumType.STRING) @Column(name = "sales_calculation_rule", nullable = false, length = 30)
    private SalesCalculationRule salesCalculationRule;
    @Column(name = "forced_carrier", length = 40) private String forcedCarrier;
    @Enumerated(EnumType.STRING) @Column(name = "vat_mode", nullable = false, length = 30) private VatMode vatMode;
    @Column(name = "price_per_volume", nullable = false) private boolean pricePerVolume;
    @Column(name = "price_per_cubic_meter", nullable = false) private boolean pricePerCubicMeter;
    @Column(name = "price_per_dimensions", nullable = false) private boolean pricePerDimensions;
    @Column(name = "price_by_bracket", nullable = false) private boolean priceByBracket;

    @Column(name = "total_volumes_min") private Integer totalVolumesMin;
    @Column(name = "total_volumes_max") private Integer totalVolumesMax;
    @Column(name = "total_weight_min_kg", precision = 10, scale = 3) private BigDecimal totalWeightMinKg;
    @Column(name = "total_weight_max_kg", precision = 10, scale = 3) private BigDecimal totalWeightMaxKg;

    @Column(name = "pickup_start") private LocalTime pickupStart;
    @Column(name = "pickup_end") private LocalTime pickupEnd;
    @Column(name = "minimum_advance_minutes") private Integer minimumAdvanceMinutes;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "associated_pickup_service_id") private OperationalService associatedPickupService;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "intercity_pickup_service_id") private OperationalService intercityPickupService;

    @Column(name = "pickup_only", nullable = false) private boolean pickupOnly;
    @Column(name = "mail_vat_zero", nullable = false) private boolean mailVatZero;
    @Column(nullable = false) private boolean international;
    @Column(name = "sea_transport", nullable = false) private boolean seaTransport;
    @Column(name = "air_transport", nullable = false) private boolean airTransport;
    @Column(nullable = false) private boolean courier;
    @Column(name = "force_import", nullable = false) private boolean forceImport;
    @Column(name = "force_export", nullable = false) private boolean forceExport;

    @Column(name = "allows_cod", nullable = false) private boolean allowsCod;
    @Column(name = "allows_return", nullable = false) private boolean allowsReturn;
    @Column(name = "allows_pudo", nullable = false) private boolean allowsPudo;
    @Column(name = "requires_delivery_pin", nullable = false) private boolean requiresDeliveryPin;

    @Column(name = "requires_email", nullable = false) private boolean requiresEmail;
    @Column(name = "auto_submit_webservice", nullable = false) private boolean autoSubmitWebservice;
    @Column(name = "requires_kilometres", nullable = false) private boolean requiresKilometres;
    @Column(name = "force_return", nullable = false) private boolean forceReturn;
    @Column(name = "no_pickup", nullable = false) private boolean noPickup;
    @Column(name = "requires_dimensions", nullable = false) private boolean requiresDimensions;
    @Column(name = "insured_value", nullable = false) private boolean insuredValue;
    @Column(name = "map_identifier", length = 120) private String mapIdentifier;

    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OperationalServiceZone> zones = new ArrayList<>();
    @OneToMany(mappedBy = "service", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OperationalServicePackageLimit> packageLimits = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "operational_service_pickup_days", joinColumns = @JoinColumn(name = "service_id"))
    @Enumerated(EnumType.STRING) @Column(name = "day_of_week", nullable = false, length = 10)
    private Set<DayOfWeek> pickupDays = new HashSet<>();
    @ElementCollection
    @CollectionTable(name = "operational_service_delivery_days", joinColumns = @JoinColumn(name = "service_id"))
    @Enumerated(EnumType.STRING) @Column(name = "day_of_week", nullable = false, length = 10)
    private Set<DayOfWeek> deliveryDays = new HashSet<>();
    @ElementCollection
    @CollectionTable(name = "operational_service_customers", joinColumns = @JoinColumn(name = "service_id"))
    @Column(name = "customer_id", nullable = false)
    private Set<UUID> exclusiveCustomerIds = new HashSet<>();
    @ElementCollection
    @CollectionTable(name = "operational_service_blocked_agencies", joinColumns = @JoinColumn(name = "service_id"))
    @Column(name = "agency", nullable = false, length = 10)
    private Set<String> blockedAgencies = new HashSet<>();

    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "created_by", nullable = false, length = 120) private String createdBy;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "updated_by", nullable = false, length = 120) private String updatedBy;
    @Version @Column(nullable = false) private long version;

    protected OperationalService() {}

    static OperationalService create(OperationalServiceDtos.Request request, ServiceGroup group,
                                     OperationalService associatedPickup, OperationalService intercityPickup,
                                     String actor) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var value = new OperationalService();
        value.id = UUID.randomUUID();
        value.createdAt = now;
        value.createdBy = actor;
        value.updatedAt = now;
        value.updatedBy = actor;
        value.configure(request, group, associatedPickup, intercityPickup);
        return value;
    }

    void update(OperationalServiceDtos.Request request, ServiceGroup group, OperationalService associatedPickup,
                OperationalService intercityPickup, String actor) {
        configure(request, group, associatedPickup, intercityPickup);
        touch(actor);
    }

    void setActive(boolean active, String actor) { this.active = active; touch(actor); }

    private void configure(OperationalServiceDtos.Request request, ServiceGroup group,
                           OperationalService associatedPickup, OperationalService intercityPickup) {
        var identity = request.identity();
        var transit = request.transit();
        var pricing = request.pricing();
        var limits = request.limits();
        var schedule = request.schedule();
        var characteristics = request.characteristics();
        var additional = request.additionalServices();
        var definitions = request.definitions();

        code = identity.code(); designation = identity.designation(); this.group = group;
        transportType = identity.transportType(); active = identity.active();
        transitMinHours = transit.transitMinHours(); transitMaxHours = transit.transitMaxHours();
        deliveryCutoff = transit.deliveryCutoff(); urgency = transit.urgency();
        priceCalculationRule = pricing.priceCalculationRule(); salesCalculationRule = pricing.salesCalculationRule();
        forcedCarrier = pricing.forcedCarrier(); vatMode = pricing.vatMode();
        pricePerVolume = pricing.pricePerVolume(); pricePerCubicMeter = pricing.pricePerCubicMeter();
        pricePerDimensions = pricing.pricePerDimensions(); priceByBracket = pricing.priceByBracket();
        totalVolumesMin = limits.totalVolumesMin(); totalVolumesMax = limits.totalVolumesMax();
        totalWeightMinKg = limits.totalWeightMinKg(); totalWeightMaxKg = limits.totalWeightMaxKg();
        pickupStart = schedule.pickupStart(); pickupEnd = schedule.pickupEnd();
        minimumAdvanceMinutes = schedule.minimumAdvanceMinutes();
        associatedPickupService = associatedPickup; intercityPickupService = intercityPickup;
        pickupOnly = characteristics.pickupOnly(); mailVatZero = characteristics.mailVatZero();
        international = characteristics.international(); seaTransport = characteristics.seaTransport();
        airTransport = characteristics.airTransport(); courier = characteristics.courier();
        forceImport = characteristics.forceImport(); forceExport = characteristics.forceExport();
        allowsCod = additional.allowsCod(); allowsReturn = additional.allowsReturn();
        allowsPudo = additional.allowsPudo(); requiresDeliveryPin = additional.requiresDeliveryPin();
        requiresEmail = definitions.requiresEmail(); autoSubmitWebservice = definitions.autoSubmitWebservice();
        requiresKilometres = definitions.requiresKilometres(); forceReturn = definitions.forceReturn();
        noPickup = definitions.noPickup(); requiresDimensions = definitions.requiresDimensions();
        insuredValue = definitions.insuredValue(); mapIdentifier = definitions.mapIdentifier();

        var requestedZoneIds = transit.zones().stream().map(OperationalServiceDtos.ZoneRuleRequest::billingZoneId)
                .collect(java.util.stream.Collectors.toSet());
        zones.removeIf(item -> !requestedZoneIds.contains(item.billingZoneId()));
        transit.zones().forEach(item -> zones.stream()
                .filter(existing -> existing.billingZoneId().equals(item.billingZoneId()))
                .findFirst().ifPresentOrElse(existing -> existing.update(item),
                        () -> zones.add(OperationalServiceZone.create(this, item))));
        var requestedPackageTypes = limits.packageLimits().stream()
                .map(OperationalServiceDtos.PackageLimitRequest::packageType)
                .collect(java.util.stream.Collectors.toSet());
        packageLimits.removeIf(item -> !requestedPackageTypes.contains(item.packageType()));
        limits.packageLimits().forEach(item -> packageLimits.stream()
                .filter(existing -> existing.packageType() == item.packageType())
                .findFirst().ifPresentOrElse(existing -> existing.update(item),
                        () -> packageLimits.add(OperationalServicePackageLimit.create(this, item))));
        pickupDays.clear(); pickupDays.addAll(schedule.pickupDays());
        deliveryDays.clear(); deliveryDays.addAll(schedule.deliveryDays());
        exclusiveCustomerIds.clear(); exclusiveCustomerIds.addAll(request.restrictions().exclusiveCustomerIds());
        blockedAgencies.clear(); blockedAgencies.addAll(request.restrictions().blockedAgencies());
    }

    private void touch(String actor) { updatedAt = OffsetDateTime.now(ZoneOffset.UTC); updatedBy = actor; }

    UUID id() { return id; } String code() { return code; } String designation() { return designation; }
    ServiceGroup group() { return group; } TransportType transportType() { return transportType; }
    boolean active() { return active; } Integer transitMinHours() { return transitMinHours; }
    Integer transitMaxHours() { return transitMaxHours; } LocalTime deliveryCutoff() { return deliveryCutoff; }
    int urgency() { return urgency; } PriceCalculationRule priceCalculationRule() { return priceCalculationRule; }
    SalesCalculationRule salesCalculationRule() { return salesCalculationRule; } String forcedCarrier() { return forcedCarrier; }
    VatMode vatMode() { return vatMode; } boolean pricePerVolume() { return pricePerVolume; }
    boolean pricePerCubicMeter() { return pricePerCubicMeter; } boolean pricePerDimensions() { return pricePerDimensions; }
    boolean priceByBracket() { return priceByBracket; } Integer totalVolumesMin() { return totalVolumesMin; }
    Integer totalVolumesMax() { return totalVolumesMax; } BigDecimal totalWeightMinKg() { return totalWeightMinKg; }
    BigDecimal totalWeightMaxKg() { return totalWeightMaxKg; } LocalTime pickupStart() { return pickupStart; }
    LocalTime pickupEnd() { return pickupEnd; } Integer minimumAdvanceMinutes() { return minimumAdvanceMinutes; }
    UUID associatedPickupServiceId() { return associatedPickupService == null ? null : associatedPickupService.id; }
    UUID intercityPickupServiceId() { return intercityPickupService == null ? null : intercityPickupService.id; }
    boolean pickupOnly() { return pickupOnly; } boolean mailVatZero() { return mailVatZero; }
    boolean international() { return international; } boolean seaTransport() { return seaTransport; }
    boolean airTransport() { return airTransport; } boolean courier() { return courier; }
    boolean forceImport() { return forceImport; } boolean forceExport() { return forceExport; }
    boolean allowsCod() { return allowsCod; } boolean allowsReturn() { return allowsReturn; }
    boolean allowsPudo() { return allowsPudo; } boolean requiresDeliveryPin() { return requiresDeliveryPin; }
    boolean requiresEmail() { return requiresEmail; } boolean autoSubmitWebservice() { return autoSubmitWebservice; }
    boolean requiresKilometres() { return requiresKilometres; } boolean forceReturn() { return forceReturn; }
    boolean noPickup() { return noPickup; } boolean requiresDimensions() { return requiresDimensions; }
    boolean insuredValue() { return insuredValue; } String mapIdentifier() { return mapIdentifier; }
    List<OperationalServiceZone> zones() { return Collections.unmodifiableList(zones); }
    List<OperationalServicePackageLimit> packageLimits() { return Collections.unmodifiableList(packageLimits); }
    Set<DayOfWeek> pickupDays() { return Set.copyOf(pickupDays); }
    Set<DayOfWeek> deliveryDays() { return Set.copyOf(deliveryDays); }
    Set<UUID> exclusiveCustomerIds() { return Set.copyOf(exclusiveCustomerIds); }
    Set<String> blockedAgencies() { return Set.copyOf(blockedAgencies); }
    OffsetDateTime createdAt() { return createdAt; } String createdBy() { return createdBy; }
    OffsetDateTime updatedAt() { return updatedAt; } String updatedBy() { return updatedBy; }
    long version() { return version; }
}
