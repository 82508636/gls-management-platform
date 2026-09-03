package pt.glsmanagement.platform.pricing;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pt.glsmanagement.platform.security.SecurityConfig;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PricingQuoteController.class)
@Import(SecurityConfig.class)
class PricingQuoteContractSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean PricingQuoteService service;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test void anonymousCannotRequestAQuote() throws Exception {
        mvc.perform(post("/internal/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(roles = "ACCOUNTING")
    void accountingCannotUseTheShipmentContract() throws Exception {
        mvc.perform(post("/internal/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "OPERATOR")
    void operatorReachesContractValidation() throws Exception {
        mvc.perform(post("/internal/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "FRONT_DESK")
    void frontDeskCanRequestAValidQuote() throws Exception {
        var payload = """
                {
                  "planId":"62b8c7d5-55a0-46dd-96f7-b7ac41e33d87",
                  "routeCode":"ROTA_PT_24H",
                  "actualWeightKg":4,
                  "parcelCount":1,
                  "lengthCm":null,
                  "widthCm":null,
                  "heightCm":null
                }
                """;
        mvc.perform(post("/internal/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isOk());
        verify(service).quote(any(PricingQuoteRequest.class));
    }

    @Test @WithMockUser(roles = "OPERATOR")
    void missingPricingDataIsReturnedAsNotFoundInsteadOfAnInternalError() throws Exception {
        when(service.quote(any(PricingQuoteRequest.class))).thenThrow(PricingException.notFound());
        var payload = """
                {
                  "planId":"62b8c7d5-55a0-46dd-96f7-b7ac41e33d87",
                  "routeCode":"INEXISTENTE",
                  "actualWeightKg":4,
                  "parcelCount":1
                }
                """;

        mvc.perform(post("/internal/v1/pricing/quotes")
                        .contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isNotFound());
    }
}
