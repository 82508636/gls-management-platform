package pt.glsmanagement.platform.pricing;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "pricing_brackets")
class PricingBracket {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "pricing_route_id") private PricingRoute route;
    @Column(name = "up_to_weight_kg", nullable = false, precision = 8, scale = 3) private BigDecimal upToWeightKg;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal price;
    @Column(name = "sort_order", nullable = false) private int sortOrder;

    protected PricingBracket() {}
    static PricingBracket create(PricingRoute route, BigDecimal upToWeight, BigDecimal price, int sortOrder) {
        var bracket = new PricingBracket();
        bracket.id = UUID.randomUUID();
        bracket.route = route;
        bracket.upToWeightKg = upToWeight;
        bracket.price = price;
        bracket.sortOrder = sortOrder;
        return bracket;
    }
    BigDecimal upToWeightKg() { return upToWeightKg; }
    BigDecimal price() { return price; }
}

