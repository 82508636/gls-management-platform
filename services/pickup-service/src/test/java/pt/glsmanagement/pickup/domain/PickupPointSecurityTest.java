package pt.glsmanagement.pickup.domain;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.glsmanagement.pickup.security.SecurityConfig;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PickupPointController.class)
@Import(SecurityConfig.class)
class PickupPointSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean PickupPointService pickupPointService;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void rejectsAnonymousReads() throws Exception {
        mockMvc.perform(get("/api/pickup-points")).andExpect(status().isUnauthorized());
    }

    @Test
    void appliesRoleChecksToGetAndHead() throws Exception {
        mockMvc.perform(get("/api/pickup-points").with(jwtWithRole("ACCOUNTING")))
                .andExpect(status().isOk());
        mockMvc.perform(head("/api/pickup-points").with(jwtWithRole("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(head("/api/pickup-points").with(jwtWithRole("FRONT_DESK")))
                .andExpect(status().isOk());
    }

    @Test
    void allowsOperationalCreationButRejectsAccounting() throws Exception {
        var json = """
                {
                  "code": "LTFT-PU-001",
                  "designation": "Ponto Operacional Fafe",
                  "address": "Rua Central 1",
                  "postalCode": "4820-001",
                  "locality": "Fafe",
                  "country": "PT",
                  "openSaturday": true,
                  "openSunday": false,
                  "active": true
                }
                """;
        mockMvc.perform(post("/api/pickup-points").with(jwtWithRole("FRONT_DESK"))
                        .contentType("application/json").content(json))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/pickup-points").with(jwtWithRole("ACCOUNTING"))
                        .contentType("application/json").content(json))
                .andExpect(status().isForbidden());
    }

    @Test
    void reservesStatusChangesAndDeletionForAdmin() throws Exception {
        var id = "00000000-0000-0000-0000-000000000001";
        mockMvc.perform(patch("/api/pickup-points/" + id + "/status")
                        .with(jwtWithRole("OPERATOR")).contentType("application/json")
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/pickup-points/" + id + "/status")
                        .with(jwtWithRole("ADMIN")).contentType("application/json")
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/pickup-points/" + id).with(jwtWithRole("OPERATOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/pickup-points/" + id).with(jwtWithRole("ADMIN")))
                .andExpect(status().isNoContent());
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtWithRole(String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of(role))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }
}
