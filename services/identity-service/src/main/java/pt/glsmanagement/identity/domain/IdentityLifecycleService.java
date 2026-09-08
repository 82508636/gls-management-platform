package pt.glsmanagement.identity.domain;

import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
class IdentityLifecycleService {
    private final KeycloakAdminClient keycloak;
    private final IdentityAuditRepository audit;
    private final IdentityAccessControl accessControl;

    IdentityLifecycleService(
            KeycloakAdminClient keycloak, IdentityAuditRepository audit, IdentityAccessControl accessControl) {
        this.keycloak = keycloak;
        this.audit = audit;
        this.accessControl = accessControl;
    }

    List<IdentityUserResponse> list() { return keycloak.listUsers(); }

    @Transactional
    IdentityUserResponse join(JoinerRequest request, Jwt actor) {
        var user = keycloak.create(request);
        try {
            accessControl.activate(user.id(), request.role());
            record(actor, "JOINER", user, "", roles(user), "Utilizador criado e ativado");
            return user;
        } catch (RuntimeException failure) {
            compensateJoiner(user.id(), failure);
            throw failure;
        }
    }

    @Transactional
    IdentityUserResponse move(String id, MoverRequest request, Jwt actor) {
        var before = keycloak.get(id);
        accessControl.activate(id, request.role());
        var user = keycloak.move(id, request.role());
        record(actor, "MOVER", user, roles(before), roles(user), "Perfis da plataforma substituídos");
        return user;
    }

    @Transactional
    IdentityUserResponse leave(String id, Jwt actor) {
        var before = keycloak.get(id);
        accessControl.disable(id);
        var user = keycloak.leave(id);
        record(actor, "LEAVER", user, roles(before), roles(user), "Utilizador desativado e sessões terminadas");
        return user;
    }

    private void record(
            Jwt actor, String action, IdentityUserResponse user, String previous, String next, String details) {
        audit.save(IdentityAuditEvent.create(
                actor.getSubject(), actor.getClaimAsString("preferred_username"), action,
                user.id(), user.username(), previous, next, details));
    }

    private void compensateJoiner(String userId, RuntimeException originalFailure) {
        try {
            keycloak.delete(userId);
        } catch (RuntimeException compensationFailure) {
            originalFailure.addSuppressed(compensationFailure);
        }
        try {
            accessControl.disable(userId);
        } catch (RuntimeException compensationFailure) {
            originalFailure.addSuppressed(compensationFailure);
        }
    }

    private static String roles(IdentityUserResponse user) {
        return user.roles().stream().map(Enum::name).sorted().reduce((left, right) -> left + "," + right)
                .orElse("");
    }
}
