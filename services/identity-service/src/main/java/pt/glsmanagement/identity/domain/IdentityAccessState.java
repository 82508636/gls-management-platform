package pt.glsmanagement.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;

@Entity
@Table(name = "identity_access_states")
class IdentityAccessState {
    @Id @Column(name = "user_id", length = 100) private String userId;
    @Enumerated(EnumType.STRING) @Column(name = "expected_role", length = 30) private PlatformRole expectedRole;
    @Column(nullable = false) private boolean enabled;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    protected IdentityAccessState() {}

    static IdentityAccessState active(String userId, PlatformRole role) {
        var state = new IdentityAccessState();
        state.userId = userId;
        state.activate(role);
        return state;
    }

    void activate(PlatformRole role) {
        expectedRole = role;
        enabled = true;
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    void disable() {
        enabled = false;
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    boolean allows(Set<PlatformRole> tokenRoles) {
        return enabled && expectedRole != null && tokenRoles.size() == 1 && tokenRoles.contains(expectedRole);
    }
}
