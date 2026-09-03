package pt.glsmanagement.platform.pricing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

interface PricingRouteRepository extends JpaRepository<PricingRoute, UUID> {
    Optional<PricingRoute> findByIdAndPlanId(UUID id, UUID planId);
    Optional<PricingRoute> findByPlanIdAndCodeIgnoreCase(UUID planId, String code);
    boolean existsByPlanIdAndCodeIgnoreCase(UUID planId, String code);
}

