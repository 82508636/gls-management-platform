package pt.glsmanagement.platform.pricing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.glsmanagement.platform.security.SecurityConfig;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PricingController.class)
@Import(SecurityConfig.class)
class PricingSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean PricingService service;

    @Test void anonymousCannotRead() throws Exception { mvc.perform(get("/api/pricing/plans")).andExpect(status().isUnauthorized()); }

    @Test @WithMockUser(roles = "ACCOUNTING")
    void accountingCanRead() throws Exception {
        when(service.list()).thenReturn(List.of());
        mvc.perform(get("/api/pricing/plans")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ACCOUNTING")
    void accountingCanUseHead() throws Exception {
        when(service.list()).thenReturn(List.of());
        mvc.perform(head("/api/pricing/plans")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "OPERATOR")
    void operatorCannotReadPricing() throws Exception { mvc.perform(get("/api/pricing/plans")).andExpect(status().isForbidden()); }

    @Test @WithMockUser(roles = "ACCOUNTING")
    void accountingCannotModifyPlans() throws Exception {
        mvc.perform(post("/api/pricing/plans").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void adminReachesPlanValidation() throws Exception {
        mvc.perform(post("/api/pricing/plans").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }
}
