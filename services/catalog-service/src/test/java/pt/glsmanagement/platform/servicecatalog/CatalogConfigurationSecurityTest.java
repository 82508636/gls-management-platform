package pt.glsmanagement.platform.servicecatalog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.glsmanagement.platform.security.SecurityConfig;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pt.glsmanagement.platform.security.SecurityTestJwt.jwtWithRole;

@WebMvcTest(ServiceGroupController.class)
@Import(SecurityConfig.class)
class CatalogConfigurationSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean ServiceGroupService groups;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test void anonymousAndDriverCannotReadConfiguration() throws Exception {
        mvc.perform(get("/api/service-groups")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/service-groups").with(jwtWithRole("DRIVER"))).andExpect(status().isForbidden());
    }

    @Test void operationalRolesCanReadAndUseHead() throws Exception {
        when(groups.list(null)).thenReturn(List.of());
        mvc.perform(get("/api/service-groups").with(jwtWithRole("OPERATOR"))).andExpect(status().isOk());
        mvc.perform(head("/api/service-groups").with(jwtWithRole("ACCOUNTING"))).andExpect(status().isOk());
        mvc.perform(get("/api/service-groups").with(jwtWithRole("FRONT_DESK"))).andExpect(status().isOk());
    }

    @Test void onlyAdminCanWriteConfiguration() throws Exception {
        mvc.perform(post("/api/service-groups").with(jwtWithRole("ACCOUNTING"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/service-groups").with(jwtWithRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test void acceptsAValidAdminGroupRequest() throws Exception {
        mvc.perform(post("/api/service-groups").with(jwtWithRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":\"PARCEL\",\"designation\":\"Encomendas\"}"))
                .andExpect(status().isCreated());
    }
}

