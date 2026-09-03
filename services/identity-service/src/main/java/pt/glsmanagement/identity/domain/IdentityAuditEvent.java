package pt.glsmanagement.identity.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "identity_audit_events")
class IdentityAuditEvent {
    @Id private UUID id;
    @Column(name = "occurred_at", nullable = false) private OffsetDateTime occurredAt;
    @Column(name = "actor_subject", nullable = false, length = 100) private String actorSubject;
    @Column(name = "actor_username", length = 200) private String actorUsername;
    @Column(nullable = false, length = 20) private String action;
    @Column(name = "target_user_id", nullable = false, length = 100) private String targetUserId;
    @Column(name = "target_username", nullable = false, length = 200) private String targetUsername;
    @Column(name = "previous_roles", length = 500) private String previousRoles;
    @Column(name = "new_roles", length = 500) private String newRoles;
    @Column(length = 500) private String details;

    protected IdentityAuditEvent() {}

    static IdentityAuditEvent create(
            String actorSubject, String actorUsername, String action, String targetUserId, String targetUsername,
            String previousRoles, String newRoles, String details) {
        var event = new IdentityAuditEvent();
        event.id = UUID.randomUUID();
        event.occurredAt = OffsetDateTime.now(ZoneOffset.UTC);
        event.actorSubject = actorSubject;
        event.actorUsername = actorUsername;
        event.action = action;
        event.targetUserId = targetUserId;
        event.targetUsername = targetUsername;
        event.previousRoles = previousRoles;
        event.newRoles = newRoles;
        event.details = details;
        return event;
    }
}
