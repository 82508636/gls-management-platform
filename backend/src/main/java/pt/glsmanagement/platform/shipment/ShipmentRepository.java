package pt.glsmanagement.platform.shipment;

import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    Page<Shipment> findByCustomerId(UUID customerId, Pageable pageable);
    List<Shipment> findByCustomerIdOrderByShipmentDateDescCreatedAtDesc(UUID customerId);
}
