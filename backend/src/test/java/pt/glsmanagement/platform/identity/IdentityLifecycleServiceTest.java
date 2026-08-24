package pt.glsmanagement.platform.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityLifecycleServiceTest {
    private final KeycloakAdminClient keycloak = mock(KeycloakAdminClient.class);
    private final IdentityAuditRepository audit = mock(IdentityAuditRepository.class);
    private final IdentityLifecycleService service = new IdentityLifecycleService(keycloak, audit);
    private final Jwt actor = Jwt.withTokenValue("token")
            .header("alg", "none")
            .subject("admin-subject")
            .claim("preferred_username", "admin.user")
            .build();

    @BeforeEach
    void resetAuditCaptorState() {
        org.mockito.Mockito.reset(keycloak, audit);
    }

    @Test
    void joinRecordsAuditEvent() {
        var request = new JoinerRequest("new.user", "new.user@example.test", "New", "User",
                PlatformRole.OPERATOR, "Temporary-Password-123!");
        var created = user("user-1", "new.user", true, PlatformRole.OPERATOR);
        when(keycloak.create(request)).thenReturn(created);

        service.join(request, actor);

        assertAudit("JOINER", "user-1", "", "OPERATOR");
    }

    @Test
    void moveRecordsPreviousAndNewRoles() {
        var before = user("user-1", "new.user", true, PlatformRole.OPERATOR);
        var after = user("user-1", "new.user", true, PlatformRole.ACCOUNTING);
        when(keycloak.get("user-1")).thenReturn(before);
        when(keycloak.move("user-1", PlatformRole.ACCOUNTING)).thenReturn(after);

        service.move("user-1", new MoverRequest(PlatformRole.ACCOUNTING), actor);

        assertAudit("MOVER", "user-1", "OPERATOR", "ACCOUNTING");
    }

    @Test
    void leaveRecordsAuditEventAndPreservesRoleHistory() {
        var before = user("user-1", "new.user", true, PlatformRole.ACCOUNTING);
        var after = user("user-1", "new.user", false, PlatformRole.ACCOUNTING);
        when(keycloak.get("user-1")).thenReturn(before);
        when(keycloak.leave("user-1")).thenReturn(after);

        service.leave("user-1", actor);

        assertAudit("LEAVER", "user-1", "ACCOUNTING", "ACCOUNTING");
    }

    private void assertAudit(String action, String targetId, String previousRoles, String newRoles) {
        var captor = ArgumentCaptor.forClass(IdentityAuditEvent.class);
        verify(audit).save(captor.capture());
        var event = captor.getValue();
        assertThat(ReflectionTestUtils.getField(event, "actorSubject")).isEqualTo("admin-subject");
        assertThat(ReflectionTestUtils.getField(event, "actorUsername")).isEqualTo("admin.user");
        assertThat(ReflectionTestUtils.getField(event, "action")).isEqualTo(action);
        assertThat(ReflectionTestUtils.getField(event, "targetUserId")).isEqualTo(targetId);
        assertThat(ReflectionTestUtils.getField(event, "previousRoles")).isEqualTo(previousRoles);
        assertThat(ReflectionTestUtils.getField(event, "newRoles")).isEqualTo(newRoles);
    }

    private static IdentityUserResponse user(String id, String username, boolean enabled, PlatformRole role) {
        return new IdentityUserResponse(id, username, username + "@example.test", "New", "User", enabled, Set.of(role));
    }
}
