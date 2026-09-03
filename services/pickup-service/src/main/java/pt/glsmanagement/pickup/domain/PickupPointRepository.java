package pt.glsmanagement.pickup.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface PickupPointRepository extends JpaRepository<PickupPoint, UUID> {
    boolean existsByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCaseAndIdNot(String code, UUID id);
}
