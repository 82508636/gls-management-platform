package pt.glsmanagement.platform.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.glsmanagement.platform.security.SecurityConfig;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerReferenceController.class)
@Import(SecurityConfig.class)
class CustomerReferenceSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean CustomerService service;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void onlyAdminCanUseTheInternalReferenceContract() throws Exception {
        var body = "{\"ids\":[\"00000000-0000-0000-0000-000000000001\"]}";
        mockMvc.perform(post("/internal/v1/customers/existence")
                        .contentType("application/json").content(body))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/internal/v1/customers/existence").with(jwtWithRole("OPERATOR"))
                        .contentType("application/json").content(body))
                .andExpect(status().isForbidden());

        when(service.allExist(anySet())).thenReturn(true);
        mockMvc.perform(post("/internal/v1/customers/existence").with(jwtWithRole("ADMIN"))
                        .contentType("application/json").content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allExist").value(true));
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtWithRole(String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of(role))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
