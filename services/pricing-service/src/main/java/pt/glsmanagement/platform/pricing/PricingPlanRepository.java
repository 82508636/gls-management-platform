package pt.glsmanagement.platform.pricing;

import org.springframework.data.jpa.repository.*;
import java.util.*;

interface PricingPlanRepository extends JpaRepository<PricingPlan, UUID> {
    boolean existsByCodeIgnoreCaseAndVersion(String code, int version);
    List<PricingPlan> findAllByOrderByUpdatedAtDesc();
}

