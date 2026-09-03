package pt.glsmanagement.platform.identity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.glsmanagement.platform.security.SecurityConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pt.glsmanagement.platform.security.SecurityTestJwt.jwtWithRole;

@WebMvcTest(IdentityAdminController.class)
@Import(SecurityConfig.class)
class IdentityAdminSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IdentityLifecycleService identityLifecycleService;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void rejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsOperatorRequests() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(jwtWithRole("OPERATOR")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsAccountingRequests() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(jwtWithRole("ACCOUNTING")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsCustomerRequests() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(jwtWithRole("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdminRequests() throws Exception {
        mockMvc.perform(get("/api/admin/users").with(jwtWithRole("ADMIN")))
                .andExpect(status().isOk());
    }
}
