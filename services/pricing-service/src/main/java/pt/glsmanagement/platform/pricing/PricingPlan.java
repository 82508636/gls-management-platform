package pt.glsmanagement.platform.pricing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "pricing_plans")
class PricingPlan {
    enum Status { DRAFT, ACTIVE, ARCHIVED }

    @Id private UUID id;
    @Column(nullable = false, length = 40) private String code;
    @Column(nullable = false, length = 160) private String designation;
    @Column(nullable = false) private int version;
    @Column(name = "valid_from", nullable = false) private LocalDate validFrom;
    @Column(name = "valid_to") private LocalDate validTo;
    @Column(nullable = false, length = 3) private String currency;
    @Column(name = "fuel_surcharge_percent", nullable = false, precision = 7, scale = 4) private BigDecimal fuelSurchargePercent;
    @Column(name = "vat_percent", nullable = false, precision = 7, scale = 4) private BigDecimal vatPercent;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private Status status;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "created_by", nullable = false, length = 120) private String createdBy;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "updated_by", nullable = false, length = 120) private String updatedBy;
    @OneToMany(mappedBy = "plan", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder asc")
    private List<PricingRoute> routes = new ArrayList<>();

    protected PricingPlan() {}

    static PricingPlan create(String code, String designation, int version, LocalDate validFrom, LocalDate validTo,
                              String currency, BigDecimal fuelPercent, BigDecimal vatPercent, String actor) {
        var plan = new PricingPlan();
        plan.id = UUID.randomUUID();
        plan.code = code;
        plan.designation = designation;
        plan.version = version;
        plan.validFrom = validFrom;
        plan.validTo = validTo;
        plan.currency = currency;
        plan.fuelSurchargePercent = fuelPercent;
        plan.vatPercent = vatPercent;
        plan.status = Status.DRAFT;
        plan.createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        plan.createdBy = actor;
        plan.updatedAt = plan.createdAt;
        plan.updatedBy = actor;
        return plan;
    }

    void update(String designation, LocalDate validFrom, LocalDate validTo, String currency,
                BigDecimal fuelPercent, BigDecimal vatPercent, String actor) {
        requireDraft();
        this.designation = designation;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.currency = currency;
        this.fuelSurchargePercent = fuelPercent;
        this.vatPercent = vatPercent;
        touch(actor);
    }

    void addRoute(PricingRoute route, String actor) { requireDraft(); routes.add(route); touch(actor); }
    void touch(String actor) { updatedAt = OffsetDateTime.now(ZoneOffset.UTC); updatedBy = actor; }
    void activate(String actor) { requireDraft(); status = Status.ACTIVE; touch(actor); }
    void archive(String actor) { if (status == Status.ARCHIVED) return; status = Status.ARCHIVED; touch(actor); }
    void requireDraft() { if (status != Status.DRAFT) throw PricingException.invalidState(); }

    UUID id() { return id; }
    String code() { return code; }
    String designation() { return designation; }
    int version() { return version; }
    LocalDate validFrom() { return validFrom; }
    LocalDate validTo() { return validTo; }
    String currency() { return currency; }
    BigDecimal fuelSurchargePercent() { return fuelSurchargePercent; }
    BigDecimal vatPercent() { return vatPercent; }
    Status status() { return status; }
    OffsetDateTime createdAt() { return createdAt; }
    String createdBy() { return createdBy; }
    OffsetDateTime updatedAt() { return updatedAt; }
    String updatedBy() { return updatedBy; }
    List<PricingRoute> routes() { return routes; }
}

