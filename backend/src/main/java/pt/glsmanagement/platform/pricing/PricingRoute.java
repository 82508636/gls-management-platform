package pt.glsmanagement.platform.pricing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "pricing_routes")
class PricingRoute {
    enum ServiceCode { BUSINESS_PARCEL, EXPRESS_PARCEL }

    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "pricing_plan_id") private PricingPlan plan;
    @Enumerated(EnumType.STRING) @Column(name = "service_code", nullable = false, length = 30) private ServiceCode serviceCode;
    @Column(nullable = false, length = 60) private String code;
    @Column(nullable = false, length = 160) private String designation;
    @Column(name = "destination_country", nullable = false, length = 2) private String destinationCountry;
    @Column(name = "delivery_commitment", nullable = false, length = 40) private String deliveryCommitment;
    @Column(name = "volumetric_factor", nullable = false, precision = 8, scale = 3) private BigDecimal volumetricFactor;
    @Column(name = "max_piece_weight_kg", nullable = false, precision = 8, scale = 3) private BigDecimal maxPieceWeightKg;
    @Column(name = "max_combined_dimensions_cm", precision = 8, scale = 2) private BigDecimal maxCombinedDimensionsCm;
    @Column(name = "additional_step_kg", nullable = false, precision = 8, scale = 3) private BigDecimal additionalStepKg;
    @Column(name = "additional_step_price", precision = 12, scale = 2) private BigDecimal additionalStepPrice;
    @Column(nullable = false) private boolean enabled;
    @Column(name = "sort_order", nullable = false) private int sortOrder;
    @OneToMany(mappedBy = "route", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("upToWeightKg asc")
    private List<PricingBracket> brackets = new ArrayList<>();

    protected PricingRoute() {}

    static PricingRoute create(PricingPlan plan, ServiceCode serviceCode, String code, String designation,
                               String destinationCountry, String commitment, BigDecimal volumetricFactor,
                               BigDecimal maxPieceWeight, BigDecimal maxDimensions, BigDecimal additionalStep,
                               BigDecimal additionalPrice, boolean enabled, int sortOrder) {
        var route = new PricingRoute();
        route.id = UUID.randomUUID();
        route.plan = plan;
        route.serviceCode = serviceCode;
        route.code = code;
        route.apply(designation, destinationCountry, commitment, volumetricFactor, maxPieceWeight,
                maxDimensions, additionalStep, additionalPrice, enabled, sortOrder);
        return route;
    }

    void update(String designation, String destinationCountry, String commitment, BigDecimal volumetricFactor,
                BigDecimal maxPieceWeight, BigDecimal maxDimensions, BigDecimal additionalStep,
                BigDecimal additionalPrice, boolean enabled, int sortOrder, List<PricingDtos.BracketRequest> values) {
        plan.requireDraft();
        apply(designation, destinationCountry, commitment, volumetricFactor, maxPieceWeight,
                maxDimensions, additionalStep, additionalPrice, enabled, sortOrder);
        replaceBrackets(values);
    }

    void replaceBrackets(List<PricingDtos.BracketRequest> values) {
        brackets.clear();
        int position = 1;
        for (var value : values) brackets.add(PricingBracket.create(this, value.upToWeightKg(), value.price(), position++));
    }

    private void apply(String designation, String destinationCountry, String commitment, BigDecimal volumetricFactor,
                       BigDecimal maxPieceWeight, BigDecimal maxDimensions, BigDecimal additionalStep,
                       BigDecimal additionalPrice, boolean enabled, int sortOrder) {
        this.designation = designation;
        this.destinationCountry = destinationCountry;
        this.deliveryCommitment = commitment;
        this.volumetricFactor = volumetricFactor;
        this.maxPieceWeightKg = maxPieceWeight;
        this.maxCombinedDimensionsCm = maxDimensions;
        this.additionalStepKg = additionalStep;
        this.additionalStepPrice = additionalPrice;
        this.enabled = enabled;
        this.sortOrder = sortOrder;
    }

    UUID id() { return id; }
    PricingPlan plan() { return plan; }
    ServiceCode serviceCode() { return serviceCode; }
    String code() { return code; }
    String designation() { return designation; }
    String destinationCountry() { return destinationCountry; }
    String deliveryCommitment() { return deliveryCommitment; }
    BigDecimal volumetricFactor() { return volumetricFactor; }
    BigDecimal maxPieceWeightKg() { return maxPieceWeightKg; }
    BigDecimal maxCombinedDimensionsCm() { return maxCombinedDimensionsCm; }
    BigDecimal additionalStepKg() { return additionalStepKg; }
    BigDecimal additionalStepPrice() { return additionalStepPrice; }
    boolean enabled() { return enabled; }
    int sortOrder() { return sortOrder; }
    List<PricingBracket> brackets() { return brackets; }
}
