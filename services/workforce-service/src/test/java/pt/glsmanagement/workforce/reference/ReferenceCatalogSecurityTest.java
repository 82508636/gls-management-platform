package pt.glsmanagement.workforce.reference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.glsmanagement.workforce.security.SecurityConfig;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReferenceCatalogController.class)
@Import(SecurityConfig.class)
class ReferenceCatalogSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ReferenceCatalogService service;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void rejectsAnonymousAndOperationalUsers() throws Exception {
        mockMvc.perform(get("/api/reference-data/account-profiles")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/reference-data/account-profiles").with(jwtWithRole("OPERATOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void explicitlyAllowsAdminGetAndHead() throws Exception {
        when(service.profiles()).thenReturn(List.of());
        mockMvc.perform(get("/api/reference-data/account-profiles").with(jwtWithRole("ADMIN")))
                .andExpect(status().isOk());
        mockMvc.perform(head("/api/reference-data/account-profiles").with(jwtWithRole("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test
    void allowsOnlyAdminWrites() throws Exception {
        var body = "{\"id\":\"AUDITOR\",\"designation\":\"Auditor\"}";
        mockMvc.perform(post("/api/reference-data/account-profiles").with(jwtWithRole("ADMIN"))
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/reference-data/account-profiles").with(jwtWithRole("ACCOUNTING"))
                        .contentType("application/json").content(body))
                .andExpect(status().isForbidden());
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtWithRole(String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of(role))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }
}
