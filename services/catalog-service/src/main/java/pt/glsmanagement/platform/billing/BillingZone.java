package pt.glsmanagement.platform.billing;

import jakarta.persistence.*;
import java.time.*;
import java.util.*;

@Entity
@Table(name = "billing_zones")
class BillingZone {
    enum ZoneType { DESTINATION_POSTAL_CODES }

    @Id private UUID id;
    @Column(nullable = false, unique = true, length = 40) private String code;
    @Column(nullable = false, length = 160) private String designation;
    @Enumerated(EnumType.STRING) @Column(name = "zone_type", nullable = false, length = 40) private ZoneType zoneType;
    @Column(nullable = false, length = 2) private String country;
    @Column(name = "group_name", length = 120) private String groupName;
    @Column(nullable = false) private boolean active;
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "billing_zone_postal_codes", joinColumns = @JoinColumn(name = "billing_zone_id"))
    @Column(name = "postal_code_pattern", nullable = false, length = 30)
    private Set<String> postalCodePatterns = new LinkedHashSet<>();
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "created_by", nullable = false, length = 120) private String createdBy;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "updated_by", nullable = false, length = 120) private String updatedBy;
    @Version @Column(nullable = false) private long version;

    protected BillingZone() {}

    static BillingZone create(String code, String designation, ZoneType type, String country, String groupName,
                              Set<String> patterns, String actor) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var value = new BillingZone();
        value.id = UUID.randomUUID();
        value.code = code;
        value.designation = designation;
        value.zoneType = type;
        value.country = country;
        value.groupName = groupName;
        value.postalCodePatterns.addAll(patterns);
        value.active = true;
        value.createdAt = now;
        value.createdBy = actor;
        value.updatedAt = now;
        value.updatedBy = actor;
        return value;
    }

    void update(String code, String designation, ZoneType type, String country, String groupName, Set<String> patterns,
                String actor) {
        this.code = code;
        this.designation = designation;
        this.zoneType = type;
        this.country = country;
        this.groupName = groupName;
        this.postalCodePatterns.clear();
        this.postalCodePatterns.addAll(patterns);
        touch(actor);
    }

    void setActive(boolean active, String actor) { this.active = active; touch(actor); }
    private void touch(String actor) { this.updatedAt = OffsetDateTime.now(ZoneOffset.UTC); this.updatedBy = actor; }

    UUID id() { return id; }
    String code() { return code; }
    String designation() { return designation; }
    ZoneType zoneType() { return zoneType; }
    String country() { return country; }
    String groupName() { return groupName; }
    boolean active() { return active; }
    Set<String> postalCodePatterns() { return Collections.unmodifiableSet(postalCodePatterns); }
    OffsetDateTime createdAt() { return createdAt; }
    String createdBy() { return createdBy; }
    OffsetDateTime updatedAt() { return updatedAt; }
    String updatedBy() { return updatedBy; }
    long version() { return version; }
}

