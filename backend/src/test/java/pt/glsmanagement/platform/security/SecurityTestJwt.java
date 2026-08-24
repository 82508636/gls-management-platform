package pt.glsmanagement.platform.security;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.Map;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

public final class SecurityTestJwt {
    private SecurityTestJwt() {
    }

    public static RequestPostProcessor jwtWithRole(String role) {
        var normalizedRole = role.toUpperCase();
        return jwt()
                .jwt(token -> token.claim("realm_access", Map.of("roles", List.of(role))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + normalizedRole));
    }
}
