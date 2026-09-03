package pt.glsmanagement.identity.domain;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/internal/v1/access-decisions")
class AccessDecisionController {
    private final IdentityAccessControl accessControl;
    AccessDecisionController(IdentityAccessControl accessControl) { this.accessControl = accessControl; }

    @GetMapping("/self")
    AccessDecision self(@AuthenticationPrincipal Jwt jwt) {
        return new AccessDecision(accessControl.isTokenAllowed(jwt.getSubject(), platformRoles(jwt)));
    }

    private static EnumSet<PlatformRole> platformRoles(Jwt jwt) {
        var result = EnumSet.noneOf(PlatformRole.class);
        Object claim = jwt.getClaim("realm_access");
        if (claim instanceof Map<?, ?> realm && realm.get("roles") instanceof Collection<?> roles) {
            roles.stream().filter(String.class::isInstance).map(String.class::cast)
                    .map(value -> value.toUpperCase(Locale.ROOT)).forEach(value -> {
                        try { result.add(PlatformRole.valueOf(value)); }
                        catch (IllegalArgumentException ignored) { }
                    });
        }
        return result;
    }

    record AccessDecision(boolean allowed) {}
}
