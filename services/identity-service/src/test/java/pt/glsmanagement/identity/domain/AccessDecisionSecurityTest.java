package pt.glsmanagement.identity.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.glsmanagement.identity.security.SecurityConfig;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({AccessDecisionController.class, IdentityAdminController.class})
@Import(SecurityConfig.class)
class AccessDecisionSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean IdentityAccessControl accessControl;
    @MockitoBean IdentityLifecycleService lifecycleService;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void protectsTheInternalDecisionEndpoint() throws Exception {
        mockMvc.perform(get("/internal/v1/access-decisions/self"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exposesDecisionToAnyAuthenticatedPlatformTokenAndSupportsHead() throws Exception {
        when(accessControl.isTokenAllowed(eq("operator-id"), anySet())).thenReturn(true);

        mockMvc.perform(get("/internal/v1/access-decisions/self").with(jwtWithRole("operator-id", "OPERATOR")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(true));
        mockMvc.perform(head("/internal/v1/access-decisions/self").with(jwtWithRole("operator-id", "OPERATOR")))
                .andExpect(status().isOk());
    }

    @Test
    void keepsAdminPagesRestrictedForGetAndHead() throws Exception {
        when(lifecycleService.list()).thenReturn(List.of());

        mockMvc.perform(get("/api/admin/users").with(jwtWithRole("operator-id", "OPERATOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(head("/api/admin/users").with(jwtWithRole("admin-id", "ADMIN")))
                .andExpect(status().isOk());
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtWithRole(
            String subject, String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.subject(subject).claim("realm_access", Map.of("roles", List.of(role))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
