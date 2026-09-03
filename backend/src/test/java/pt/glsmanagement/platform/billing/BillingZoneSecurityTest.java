package pt.glsmanagement.platform.billing;

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

@WebMvcTest(BillingZoneController.class)
@Import(SecurityConfig.class)
class BillingZoneSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean BillingZoneService service;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test void anonymousAndDriverCannotReadZones() throws Exception {
        mvc.perform(get("/api/billing/zones")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/billing/zones").with(jwtWithRole("DRIVER"))).andExpect(status().isForbidden());
    }

    @Test void operationalRolesCanReadAndUseHead() throws Exception {
        when(service.list(null)).thenReturn(List.of());
        mvc.perform(get("/api/billing/zones").with(jwtWithRole("FRONT_DESK"))).andExpect(status().isOk());
        mvc.perform(head("/api/billing/zones").with(jwtWithRole("ACCOUNTING"))).andExpect(status().isOk());
    }

    @Test void onlyAdminCanWriteZones() throws Exception {
        mvc.perform(post("/api/billing/zones").with(jwtWithRole("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/billing/zones").with(jwtWithRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test void acceptsAValidAdminZoneRequest() throws Exception {
        var payload = """
                {"code":"PT-NORTE","designation":"Norte","zoneType":"DESTINATION_POSTAL_CODES",
                 "country":"PT","groupName":"Continente","postalCodePatterns":["4000-*","4100-000"]}
                """;
        mvc.perform(post("/api/billing/zones").with(jwtWithRole("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
    }
}
