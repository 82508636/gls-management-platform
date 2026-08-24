package pt.glsmanagement.platform.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {
    private final SecurityConfig.KeycloakRealmRoleConverter converter =
            new SecurityConfig.KeycloakRealmRoleConverter();

    @Test
    void mapsKeycloakRealmRolesToUppercaseSpringAuthorities() {
        var jwt = jwt(Map.of("roles", List.of("admin", "OPERATOR")));

        assertThat(converter.convert(jwt))
                .extracting("authority")
                .containsExactly("ROLE_ADMIN", "ROLE_OPERATOR");
    }

    @Test
    void returnsNoAuthoritiesWhenRealmAccessIsMissing() {
        var jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "test-user")
                .build();

        assertThat(converter.convert(jwt)).isEmpty();
    }

    @Test
    void ignoresMalformedAndNonStringRoles() {
        var malformedRealmAccess = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", "not-an-object")
                .build();
        var mixedRoles = jwt(Map.of("roles", List.of("ACCOUNTING", 123, false)));

        assertThat(converter.convert(malformedRealmAccess)).isEmpty();
        assertThat(converter.convert(mixedRoles))
                .extracting("authority")
                .containsExactly("ROLE_ACCOUNTING");
    }

    private static Jwt jwt(Map<String, Object> realmAccess) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("realm_access", realmAccess)
                .build();
    }
}
