package pt.glsmanagement.platform.servicecatalog;

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
import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OperationalServiceController.class)
@Import(SecurityConfig.class)
class OperationalServiceSecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean OperationalServiceService service;
    @MockitoBean JwtDecoder jwtDecoder;

    @Test void anonymousCannotRead() throws Exception {
        mvc.perform(get("/api/operational-services")).andExpect(status().isUnauthorized());
    }

    @Test @WithMockUser(roles = "OPERATOR")
    void operatorCanReadAndUseHead() throws Exception {
        when(service.list(null)).thenReturn(List.of());
        mvc.perform(get("/api/operational-services")).andExpect(status().isOk());
        mvc.perform(head("/api/operational-services")).andExpect(status().isOk());
    }

    @Test @WithMockUser(roles = "ACCOUNTING")
    void accountingCannotCreate() throws Exception {
        mvc.perform(post("/api/operational-services").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void adminReachesRequestValidation() throws Exception {
        mvc.perform(post("/api/operational-services").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test @WithMockUser(roles = "ADMIN")
    void acceptsTheCompleteFrontendContract() throws Exception {
        var payload = """
                {
                  "identity":{"code":"BP24","designation":"Business Parcel","groupId":"PARCEL","transportType":"SMALL_VOLUMES","active":true},
                  "transit":{"transitMinHours":1,"transitMaxHours":24,"deliveryCutoff":"18:00","urgency":3,"zones":[]},
                  "pricing":{"priceCalculationRule":"WEIGHT","salesCalculationRule":"DEFAULT","forcedCarrier":null,"vatMode":"AUTO","pricePerVolume":false,"pricePerCubicMeter":false,"pricePerDimensions":false,"priceByBracket":true},
                  "limits":{"packageLimits":[{"packageType":"BOX","maxWeightKg":40,"maxLengthCm":120,"maxWidthCm":80,"maxHeightCm":80,"maxCombinedCm":280}],"totalVolumesMin":1,"totalVolumesMax":20,"totalWeightMinKg":1,"totalWeightMaxKg":400},
                  "schedule":{"pickupStart":"09:00","pickupEnd":"18:00","minimumAdvanceMinutes":60,"pickupDays":["MONDAY"],"deliveryDays":["TUESDAY"]},
                  "pickupRules":{"associatedPickupServiceId":null,"intercityPickupServiceId":null},
                  "characteristics":{"pickupOnly":false,"mailVatZero":false,"international":false,"seaTransport":false,"airTransport":false,"courier":false,"forceImport":false,"forceExport":false},
                  "additionalServices":{"allowsCod":true,"allowsReturn":true,"allowsPudo":true,"requiresDeliveryPin":false},
                  "definitions":{"requiresEmail":true,"autoSubmitWebservice":false,"requiresKilometres":false,"forceReturn":false,"noPickup":false,"requiresDimensions":true,"insuredValue":false,"mapIdentifier":null},
                  "restrictions":{"exclusiveCustomerIds":[],"blockedAgencies":["LTFT02"]}
                }
                """;
        mvc.perform(post("/api/operational-services").contentType(MediaType.APPLICATION_JSON).content(payload))
                .andExpect(status().isCreated());
        verify(service).create(any(), anyString());
    }
}
