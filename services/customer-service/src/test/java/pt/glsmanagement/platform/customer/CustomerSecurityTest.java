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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import(SecurityConfig.class)
class CustomerSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean CustomerService customerService;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void rejectsAnonymousReadsAndUnknownRoles() throws Exception {
        mockMvc.perform(get("/api/customers")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/customers").with(jwtWithRole("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void appliesReadRolesToGetAndHead() throws Exception {
        mockMvc.perform(get("/api/customers").with(jwtWithRole("ACCOUNTING")))
                .andExpect(status().isOk());
        mockMvc.perform(head("/api/customers").with(jwtWithRole("FRONT_DESK")))
                .andExpect(status().isOk());
        mockMvc.perform(head("/api/customers").with(jwtWithRole("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsOperationalWritesButRejectsAccounting() throws Exception {
        var json = validCustomerJson();
        mockMvc.perform(post("/api/customers").with(jwtWithRole("FRONT_DESK"))
                        .contentType("application/json").content(json))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/customers").with(jwtWithRole("ACCOUNTING"))
                        .contentType("application/json").content(json))
                .andExpect(status().isForbidden());
        mockMvc.perform(put("/api/customers/00000000-0000-0000-0000-000000000001")
                        .with(jwtWithRole("OPERATOR")).contentType("application/json").content(json))
                .andExpect(status().isOk());
    }

    @Test
    void reservesActivationAndDeletionForAdmin() throws Exception {
        var id = "00000000-0000-0000-0000-000000000001";
        mockMvc.perform(patch("/api/customers/" + id + "/status")
                        .with(jwtWithRole("OPERATOR")).contentType("application/json")
                        .content("{\"active\":true}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/customers/" + id + "/status")
                        .with(jwtWithRole("ADMIN")).contentType("application/json")
                        .content("{\"active\":true}"))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/customers/" + id).with(jwtWithRole("OPERATOR")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/api/customers/" + id).with(jwtWithRole("ADMIN")))
                .andExpect(status().isNoContent());
    }

    private static SecurityMockMvcRequestPostProcessors.JwtRequestPostProcessor jwtWithRole(String role) {
        return SecurityMockMvcRequestPostProcessors.jwt()
                .jwt(jwt -> jwt.claim("realm_access", Map.of("roles", List.of(role))))
                .authorities(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    private static String validCustomerJson() {
        return """
                {
                  "shippingName":"Cliente Teste",
                  "agency":"LTFT01",
                  "address":"Rua do Cliente 1",
                  "postalCode":"4800-001",
                  "locality":"Guimarães",
                  "country":"PT",
                  "billingCountry":"PT",
                  "vatNumber":"501234567",
                  "customerType":"COMPANY",
                  "invoiceByPost":false,
                  "documentsByEmail":true,
                  "active":true
                }
                """;
    }
}
