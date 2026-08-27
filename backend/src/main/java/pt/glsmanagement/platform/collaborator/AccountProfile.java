package pt.glsmanagement.platform.collaborator;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "account_profiles")
class AccountProfile {
    @Id @Column(length = 40) private String id;
    @Column(nullable = false, length = 120) private String designation;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false) private OffsetDateTime createdAt;
    @Column(name = "created_by", nullable = false, length = 200) private String createdBy;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;
    @Column(name = "updated_by", nullable = false, length = 200) private String updatedBy;

    protected AccountProfile() {}

    static AccountProfile create(String id, String designation, String actor) {
        var now = OffsetDateTime.now(ZoneOffset.UTC);
        var profile = new AccountProfile();
        profile.id = id;
        profile.designation = designation;
        profile.active = true;
        profile.createdAt = now;
        profile.createdBy = actor;
        profile.updatedAt = now;
        profile.updatedBy = actor;
        return profile;
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
}
