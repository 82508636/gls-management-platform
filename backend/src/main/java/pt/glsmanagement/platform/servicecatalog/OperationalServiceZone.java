package pt.glsmanagement.platform.servicecatalog;

import jakarta.persistence.*;
import java.util.*;

@Entity
@Table(name = "operational_service_zones")
class OperationalServiceZone {
    @Id private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "service_id") private OperationalService service;
    @Column(name = "billing_zone_id", nullable = false) private UUID billingZoneId;
    @Column(name = "transit_min_hours") private Integer transitMinHours;
    @Column(name = "transit_max_hours") private Integer transitMaxHours;

    protected OperationalServiceZone() {}
    static OperationalServiceZone create(OperationalService service, OperationalServiceDtos.ZoneRuleRequest request) {
        var value = new OperationalServiceZone();
        value.id = UUID.randomUUID();
        value.service = service;
        value.billingZoneId = request.billingZoneId();
        value.transitMinHours = request.transitMinHours();
        value.transitMaxHours = request.transitMaxHours();
        return value;
    }
    void update(OperationalServiceDtos.ZoneRuleRequest request) {
        transitMinHours = request.transitMinHours();
        transitMaxHours = request.transitMaxHours();
    }
    UUID billingZoneId() { return billingZoneId; }
    Integer transitMinHours() { return transitMinHours; }
    Integer transitMaxHours() { return transitMaxHours; }
}
