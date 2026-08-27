package pt.glsmanagement.platform.collaborator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.glsmanagement.platform.security.SecurityConfig;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pt.glsmanagement.platform.security.SecurityTestJwt.jwtWithRole;

@WebMvcTest(ReferenceCatalogController.class)
@Import(SecurityConfig.class)
class ReferenceCatalogSecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ReferenceCatalogService service;

    @Test void rejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/reference-data/account-profiles")).andExpect(status().isUnauthorized());
    }

    @Test void rejectsOperatorRequests() throws Exception {
        mockMvc.perform(get("/api/reference-data/account-profiles").with(jwtWithRole("OPERATOR")))
                .andExpect(status().isForbidden());
    }

    @Test void allowsAdminRequests() throws Exception {
        when(service.profiles()).thenReturn(List.of());
        mockMvc.perform(get("/api/reference-data/account-profiles").with(jwtWithRole("ADMIN")))
                .andExpect(status().isOk());
    }

    @Test void authorizesHeadExplicitlyForAdmin() throws Exception {
        when(service.profiles()).thenReturn(List.of());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.head(
                "/api/reference-data/account-profiles").with(jwtWithRole("ADMIN")))
                .andExpect(status().isOk());
    }
}
