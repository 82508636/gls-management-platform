package pt.glsmanagement.platform.billing;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;

interface BillingZoneRepository extends JpaRepository<BillingZone, UUID> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
    boolean existsByDesignationIgnoreCase(String designation);
    boolean existsByDesignationIgnoreCaseAndIdNot(String designation, UUID id);
    List<BillingZone> findAllByActiveOrderByDesignation(boolean active);
    long countByIdInAndActiveTrue(Set<UUID> ids);
}

