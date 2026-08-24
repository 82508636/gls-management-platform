package pt.glsmanagement.platform.entity;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.glsmanagement.platform.security.SecurityConfig;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static pt.glsmanagement.platform.security.SecurityTestJwt.jwtWithRole;

@WebMvcTest({PickupPointController.class, RecipientController.class})
@Import(SecurityConfig.class)
class EntitySecurityTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean PickupPointService pickupPointService;
    @MockitoBean RecipientRegistrationService recipientRegistrationService;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test void rejectsAnonymousRecipientList() throws Exception {
        mockMvc.perform(get("/api/recipients")).andExpect(status().isUnauthorized());
    }

    @Test void allowsAccountingToReadPickupPoints() throws Exception {
        mockMvc.perform(get("/api/pickup-points").with(jwtWithRole("ACCOUNTING"))).andExpect(status().isOk());
    }

    @Test void doesNotExposeManualRecipientCreationEvenToAdmin() throws Exception {
        mockMvc.perform(post("/api/recipients").with(jwtWithRole("ADMIN")).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test void rejectsAccountingPickupCreation() throws Exception {
        mockMvc.perform(post("/api/pickup-points").with(jwtWithRole("ACCOUNTING")).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test void rejectsOperatorPickupStatusChange() throws Exception {
        mockMvc.perform(patch("/api/pickup-points/00000000-0000-0000-0000-000000000001/status")
                        .with(jwtWithRole("OPERATOR")).contentType("application/json").content("{\"active\":false}"))
                .andExpect(status().isForbidden());
    }

    @Test void allowsAdminPickupStatusChange() throws Exception {
        mockMvc.perform(patch("/api/pickup-points/00000000-0000-0000-0000-000000000001/status")
                        .with(jwtWithRole("ADMIN")).contentType("application/json").content("{\"active\":false}"))
                .andExpect(status().isOk());
    }
}
