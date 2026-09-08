package pt.glsmanagement.identity.domain;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityLifecycleServiceTest {
    private final KeycloakAdminClient keycloak = mock(KeycloakAdminClient.class);
    private final IdentityAuditRepository audit = mock(IdentityAuditRepository.class);
    private final IdentityAccessControl accessControl = mock(IdentityAccessControl.class);
    private final IdentityLifecycleService service = new IdentityLifecycleService(keycloak, audit, accessControl);
    private final Jwt actor = Jwt.withTokenValue("token").header("alg", "none")
            .subject("admin-subject").claim("preferred_username", "admin.user").build();

    @Test
    void joinDeletesTheKeycloakUserWhenLocalPersistenceFails() {
        var request = new JoinerRequest("new.user", "new.user@example.test", "New", "User",
                PlatformRole.OPERATOR, "Temporary-Password-123!");
        var created = new IdentityUserResponse("user-1", "new.user", "new.user@example.test",
                "New", "User", true, Set.of(PlatformRole.OPERATOR));
        when(keycloak.create(request)).thenReturn(created);
        doThrow(new IllegalStateException("database unavailable"))
                .when(accessControl).activate("user-1", PlatformRole.OPERATOR);

        assertThatThrownBy(() -> service.join(request, actor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        verify(keycloak).delete("user-1");
        verify(accessControl).disable("user-1");
    }

    @Test
    void moveChangesLocalAccessBeforeKeycloakToFailClosed() {
        var before = new IdentityUserResponse("user-1", "new.user", "new.user@example.test",
                "New", "User", true, Set.of(PlatformRole.OPERATOR));
        var request = new MoverRequest(PlatformRole.ACCOUNTING);
        when(keycloak.get("user-1")).thenReturn(before);
        doThrow(new IllegalStateException("keycloak unavailable"))
                .when(keycloak).move("user-1", PlatformRole.ACCOUNTING);

        assertThatThrownBy(() -> service.move("user-1", request, actor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("keycloak unavailable");

        var order = inOrder(accessControl, keycloak);
        order.verify(accessControl).activate("user-1", PlatformRole.ACCOUNTING);
        order.verify(keycloak).move("user-1", PlatformRole.ACCOUNTING);
    }

    @Test
    void leaveDisablesLocalAccessBeforeRevokingKeycloak() {
        var before = new IdentityUserResponse("user-1", "new.user", "new.user@example.test",
                "New", "User", true, Set.of(PlatformRole.OPERATOR));
        var disabled = new IdentityUserResponse("user-1", "new.user", "new.user@example.test",
                "New", "User", false, Set.of(PlatformRole.OPERATOR));
        when(keycloak.get("user-1")).thenReturn(before);
        when(keycloak.leave("user-1")).thenReturn(disabled);

        service.leave("user-1", actor);

        var order = inOrder(accessControl, keycloak);
        order.verify(accessControl).disable("user-1");
        order.verify(keycloak).leave("user-1");
    }

    @Test
    void leaveDoesNotTouchKeycloakWhenTheFailClosedDecisionCannotBePersisted() {
        var before = new IdentityUserResponse("user-1", "new.user", "new.user@example.test",
                "New", "User", true, Set.of(PlatformRole.OPERATOR));
        when(keycloak.get("user-1")).thenReturn(before);
        doThrow(new IllegalStateException("database unavailable"))
                .when(accessControl).disable("user-1");

        assertThatThrownBy(() -> service.leave("user-1", actor))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("database unavailable");

        verify(keycloak, never()).leave("user-1");
    }
}
