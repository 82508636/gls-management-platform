package pt.glsmanagement.platform.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import pt.glsmanagement.platform.identity.*;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class IdentityAccessTokenValidatorTest {
    private final IdentityAccessControl control = mock(IdentityAccessControl.class);
    private final IdentityAccessTokenValidator validator = new IdentityAccessTokenValidator(control);

    @Test void acceptsCurrentRoleAndRejectsRevokedRole() {
        var jwt = jwt("user-1", List.of("OPERATOR", "offline_access"));
        when(control.isTokenAllowed("user-1", Set.of(PlatformRole.OPERATOR))).thenReturn(true);
        assertThat(validator.validate(jwt).hasErrors()).isFalse();

        when(control.isTokenAllowed("user-1", Set.of(PlatformRole.OPERATOR))).thenReturn(false);
        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    @Test void ignoresNonPlatformRealmRoles() {
        var jwt = jwt("user-1", List.of("uma_authorization", "DRIVER"));
        when(control.isTokenAllowed("user-1", Set.of(PlatformRole.DRIVER))).thenReturn(true);
        assertThat(validator.validate(jwt).hasErrors()).isFalse();
    }

    private static Jwt jwt(String subject, List<String> roles) {
        return Jwt.withTokenValue("token").header("alg", "none").subject(subject)
                .claim("realm_access", Map.of("roles", roles)).build();
    }
}
