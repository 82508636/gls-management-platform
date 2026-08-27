package pt.glsmanagement.platform.security;

import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import pt.glsmanagement.platform.identity.IdentityAccessControl;
import pt.glsmanagement.platform.identity.PlatformRole;
import java.util.*;

final class IdentityAccessTokenValidator implements OAuth2TokenValidator<Jwt> {
    private static final OAuth2Error REVOKED = new OAuth2Error("invalid_token", "The token is no longer active", null);
    private final IdentityAccessControl accessControl;

    IdentityAccessTokenValidator(IdentityAccessControl accessControl) {
        this.accessControl = accessControl;
    }

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        var roles = platformRoles(jwt);
        return accessControl.isTokenAllowed(jwt.getSubject(), roles)
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(REVOKED);
    }

    private static Set<PlatformRole> platformRoles(Jwt jwt) {
        var result = EnumSet.noneOf(PlatformRole.class);
        Object claim = jwt.getClaim("realm_access");
        if (claim instanceof Map<?, ?> realm && realm.get("roles") instanceof Collection<?> roles) {
            roles.stream().filter(String.class::isInstance).map(String.class::cast)
                    .map(String::toUpperCase).forEach(role -> {
                        try { result.add(PlatformRole.valueOf(role)); } catch (IllegalArgumentException ignored) { }
                    });
        }
        return result;
    }
}
