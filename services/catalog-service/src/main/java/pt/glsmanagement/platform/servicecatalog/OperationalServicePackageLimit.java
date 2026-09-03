package pt.glsmanagement.platform.servicecatalog;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.*;

@Entity
@Table(name = "operational_service_package_limits")
class OperationalServicePackageLimit {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "service_id") private OperationalService service;
    @Enumerated(EnumType.STRING) @Column(name = "package_type", nullable = false, length = 20)
    private OperationalService.PackageType packageType;
    @Column(name = "max_weight_kg", precision = 10, scale = 3) private BigDecimal maxWeightKg;
    @Column(name = "max_length_cm", precision = 10, scale = 2) private BigDecimal maxLengthCm;
    @Column(name = "max_width_cm", precision = 10, scale = 2) private BigDecimal maxWidthCm;
    @Column(name = "max_height_cm", precision = 10, scale = 2) private BigDecimal maxHeightCm;
    @Column(name = "max_combined_cm", precision = 10, scale = 2) private BigDecimal maxCombinedCm;

    protected OperationalServicePackageLimit() {}
    static OperationalServicePackageLimit create(OperationalService service, OperationalServiceDtos.PackageLimitRequest request) {
        var value = new OperationalServicePackageLimit();
        value.id = UUID.randomUUID();
        value.service = service;
        value.packageType = request.packageType();
        value.maxWeightKg = request.maxWeightKg();
        value.maxLengthCm = request.maxLengthCm();
        value.maxWidthCm = request.maxWidthCm();
        value.maxHeightCm = request.maxHeightCm();
        value.maxCombinedCm = request.maxCombinedCm();
        return value;
    }
    void update(OperationalServiceDtos.PackageLimitRequest request) {
        maxWeightKg = request.maxWeightKg();
        maxLengthCm = request.maxLengthCm();
        maxWidthCm = request.maxWidthCm();
        maxHeightCm = request.maxHeightCm();
        maxCombinedCm = request.maxCombinedCm();
    }
    OperationalService.PackageType packageType() { return packageType; }
    BigDecimal maxWeightKg() { return maxWeightKg; }
    BigDecimal maxLengthCm() { return maxLengthCm; }
    BigDecimal maxWidthCm() { return maxWidthCm; }
    BigDecimal maxHeightCm() { return maxHeightCm; }
    BigDecimal maxCombinedCm() { return maxCombinedCm; }
}

