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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VatValidationController.class)
@Import(SecurityConfig.class)
class VatValidationSecurityTest {
    private static final String VALID_REQUEST = """
            {"countryCode":"PT","vatNumber":"500000000","subjectType":"COMPANY"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private VatValidationService service;

    @MockitoBean
    private JwtDecoder jwtDecoder;

    @Test
    void rejectsAnonymousRequests() throws Exception {
        mockMvc.perform(post("/api/vat-validations")
                        .contentType("application/json")
                        .content(VALID_REQUEST))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void allowsAdminAndOperator() throws Exception {
        mockMvc.perform(post("/api/vat-validations")
                        .with(jwtWithRole("ADMIN"))
                        .contentType("application/json")
                        .content(VALID_REQUEST))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/vat-validations")
                        .with(jwtWithRole("OPERATOR"))
                        .contentType("application/json")
                        .content(VALID_REQUEST))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAccountingAndCustomer() throws Exception {
        mockMvc.perform(post("/api/vat-validations")
                        .with(jwtWithRole("ACCOUNTING"))
                        .contentType("application/json")
                        .content(VALID_REQUEST))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/vat-validations")
                        .with(jwtWithRole("CUSTOMER"))
                        .contentType("application/json")
                        .content(VALID_REQUEST))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsAGenericValidationError() throws Exception {
        mockMvc.perform(post("/api/vat-validations")
                        .with(jwtWithRole("ADMIN"))
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Não foi possível validar os dados enviados."));
    }

}
