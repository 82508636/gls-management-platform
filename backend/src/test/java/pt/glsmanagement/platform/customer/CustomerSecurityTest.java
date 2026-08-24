package pt.glsmanagement.platform.customer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.glsmanagement.platform.security.SecurityConfig;

import static pt.glsmanagement.platform.security.SecurityTestJwt.jwtWithRole;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@Import(SecurityConfig.class)
class CustomerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CustomerService customerService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void rejectsAnonymousCustomerRequests() throws Exception {
        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsAccountingToReadCustomers() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .with(jwtWithRole("ACCOUNTING")))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsCustomerRoleFromGeneralCustomerList() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .with(jwtWithRole("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsAccountingCustomerCreation() throws Exception {
        mockMvc.perform(post("/api/customers")
                        .with(jwtWithRole("ACCOUNTING"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsOperatorCustomerStatusChange() throws Exception {
        mockMvc.perform(patch("/api/customers/00000000-0000-0000-0000-000000000001/status")
                        .with(jwtWithRole("OPERATOR"))
                        .contentType("application/json")
                        .content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminCustomerStatusChange() throws Exception {
        mockMvc.perform(patch("/api/customers/00000000-0000-0000-0000-000000000001/status")
                        .with(jwtWithRole("ADMIN"))
                        .contentType("application/json")
                        .content("{\"active\":false}"))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsStatusChangeWithoutAnExplicitState() throws Exception {
        mockMvc.perform(patch("/api/customers/00000000-0000-0000-0000-000000000001/status")
                        .with(jwtWithRole("ADMIN"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAccountingCustomerDeletion() throws Exception {
        mockMvc.perform(delete("/api/customers/00000000-0000-0000-0000-000000000001")
                        .with(jwtWithRole("ACCOUNTING")))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminCustomerDeletion() throws Exception {
        mockMvc.perform(delete("/api/customers/00000000-0000-0000-0000-000000000001")
                        .with(jwtWithRole("ADMIN")))
                .andExpect(status().isNoContent());
    }

}
