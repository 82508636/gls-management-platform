package pt.glsmanagement.platform.servicecatalog;

import jakarta.persistence.*;
import java.time.*;

@Entity
@Table(name = "service_groups")
class ServiceGroup {
    @Id @Column(length = 40) private String id;
    @Column(nullable = false, unique = true, length = 160) private String designation;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "created_by", nullable = false, length = 120) private String createdBy;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "updated_by", nullable = false, length = 120) private String updatedBy;
    @Version @Column(nullable = false) private long version;

    protected ServiceGroup() {}

    static ServiceGroup create(String id, String designation, String actor) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var value = new ServiceGroup();
        value.id = id;
        value.designation = designation;
        value.active = true;
        value.createdAt = now;
        value.createdBy = actor;
        value.updatedAt = now;
        value.updatedBy = actor;
        return value;
    }

    void update(String designation, String actor) { this.designation = designation; touch(actor); }
    void setActive(boolean active, String actor) { this.active = active; touch(actor); }
    private void touch(String actor) { updatedAt = OffsetDateTime.now(ZoneOffset.UTC); updatedBy = actor; }

    String id() { return id; }
    String designation() { return designation; }
    boolean active() { return active; }
    OffsetDateTime createdAt() { return createdAt; }
    String createdBy() { return createdBy; }
    OffsetDateTime updatedAt() { return updatedAt; }
    String updatedBy() { return updatedBy; }
    long version() { return version; }
}
