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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("É necessário iniciar sessão para continuar."));
    }

    @Test
    void allowsAccountingToReadCustomers() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .with(jwtWithRole("ACCOUNTING")))
                .andExpect(status().isOk());
    }

    @Test
    void passesPaginationSearchAndStateFiltersToTheService() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .param("page", "2").param("size", "25")
                        .param("query", "Norte").param("active", "true")
                        .with(jwtWithRole("ACCOUNTING")))
                .andExpect(status().isOk());

        verify(customerService).list(2, 25, "Norte", true);
    }

    @Test
    void rejectsCustomerRoleFromGeneralCustomerList() throws Exception {
        mockMvc.perform(get("/api/customers")
                        .with(jwtWithRole("CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Não tem permissão para realizar esta operação."));
    }

    @Test
    void returnsGenericErrorsForMalformedAndUnexpectedFailures() throws Exception {
        mockMvc.perform(post("/api/customers").with(jwtWithRole("ADMIN"))
                        .contentType("application/json").content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Não foi possível validar os dados enviados."));

        when(customerService.list(anyInt(), anyInt(), nullable(String.class), nullable(Boolean.class)))
                .thenThrow(new RuntimeException("database password leaked"));
        mockMvc.perform(get("/api/customers").with(jwtWithRole("ADMIN")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Não foi possível concluir o pedido."))
                .andExpect(result -> org.assertj.core.api.Assertions.assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("database password leaked"));
    }

    @Test
    void appliesReadAuthorizationToHeadRequests() throws Exception {
        mockMvc.perform(head("/api/customers").with(jwtWithRole("CUSTOMER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(head("/api/customers").with(jwtWithRole("ACCOUNTING")))
                .andExpect(status().isOk());
    }

    @Test
    void allowsFrontDeskToUseCustomerOperations() throws Exception {
        mockMvc.perform(get("/api/customers").with(jwtWithRole("FRONT_DESK")))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/customers").with(jwtWithRole("FRONT_DESK"))
                        .contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deniesDriverAndUnspecifiedApiMethods() throws Exception {
        mockMvc.perform(get("/api/customers").with(jwtWithRole("DRIVER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options("/api/customers")
                        .with(jwtWithRole("DRIVER")))
                .andExpect(status().isOk());
        mockMvc.perform(patch("/api/customers/00000000-0000-0000-0000-000000000001/unsupported")
                        .with(jwtWithRole("ADMIN")))
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
