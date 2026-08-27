package pt.glsmanagement.platform.shipment;

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
import java.util.UUID;

import static pt.glsmanagement.platform.security.SecurityTestJwt.jwtWithRole;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ShipmentController.class)
@Import(SecurityConfig.class)
class ShipmentSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean ShipmentService service;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test
    void anonymousCannotReadShipments() throws Exception {
        mvc.perform(get("/api/shipments")).andExpect(status().isUnauthorized());
    }

    @Test
    void allowedOperationalRolesCanReadAndUseHead() throws Exception {
        when(service.list(anyInt(), anyInt())).thenReturn(emptyPage());
        mvc.perform(get("/api/shipments").with(jwtWithRole("ACCOUNTING"))).andExpect(status().isOk());
        mvc.perform(head("/api/shipments").with(jwtWithRole("OPERATOR"))).andExpect(status().isOk());
    }

    @Test
    void operatorAndFrontDeskReachCreationValidation() throws Exception {
        mvc.perform(post("/api/shipments").with(jwtWithRole("OPERATOR"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/shipments").with(jwtWithRole("FRONT_DESK"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void accountingCannotCreateShipments() throws Exception {
        mvc.perform(post("/api/shipments").with(jwtWithRole("ACCOUNTING"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void accountingCanReadCustomerAccountServicesButCustomerCannot() throws Exception {
        var customerId = UUID.randomUUID();
        when(service.customerServices(customerId)).thenReturn(List.of());
        mvc.perform(get("/api/customers/{id}/services", customerId).with(jwtWithRole("ACCOUNTING")))
                .andExpect(status().isOk());
        mvc.perform(get("/api/customers/{id}/services", customerId).with(jwtWithRole("CUSTOMER")))
                .andExpect(status().isForbidden());
    }

    private static ShipmentPageResponse emptyPage() {
        return new ShipmentPageResponse(List.of(), 0, 50, 0, 0, true, true);
    }
}
