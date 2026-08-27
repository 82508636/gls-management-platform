package pt.glsmanagement.platform.pricing;

import org.junit.jupiter.api.*;
import org.mockito.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static pt.glsmanagement.platform.pricing.PricingDtos.*;

class PricingServiceTest {
    private PricingPlanRepository plans;
    private PricingRouteRepository routes;
    private PricingService service;
    private PricingPlan plan;
    private PricingRoute route;

    @BeforeEach
    void setUp() {
        plans = mock(PricingPlanRepository.class);
        routes = mock(PricingRouteRepository.class);
        service = new PricingService(plans, routes);
        plan = PricingPlan.create("TEST", "Tabela teste", 1, LocalDate.of(2026, 1, 1), null,
                "EUR", new BigDecimal("7.0000"), new BigDecimal("23.0000"), "tester");
        route = PricingRoute.create(plan, PricingRoute.ServiceCode.BUSINESS_PARCEL, "BUS_PT_24H",
                "Portugal 24h", "PT", "24h", new BigDecimal("167"), new BigDecimal("40"),
                new BigDecimal("300"), BigDecimal.ONE, new BigDecimal("0.36"), true, 1);
        route.replaceBrackets(List.of(
                new BracketRequest(BigDecimal.ONE, new BigDecimal("4.10")),
                new BracketRequest(new BigDecimal("5"), new BigDecimal("4.56")),
                new BracketRequest(new BigDecimal("30"), new BigDecimal("9.52"))));
        plan.addRoute(route, "tester");
        when(plans.findById(plan.id())).thenReturn(Optional.of(plan));
        when(routes.findByPlanIdAndCodeIgnoreCase(plan.id(), "BUS_PT_24H")).thenReturn(Optional.of(route));
    }

    @Test
    void calculatesBracketFuelVatAndTotal() {
        var result = service.simulate(new SimulationRequest(plan.id(), "bus_pt_24h", new BigDecimal("4"), 1,
                null, null, null));
        assertThat(result.chargeableWeightKg()).isEqualByComparingTo("4.000");
        assertThat(result.basePrice()).isEqualByComparingTo("4.56");
        assertThat(result.fuelSurcharge()).isEqualByComparingTo("0.32");
        assertThat(result.vat()).isEqualByComparingTo("1.12");
        assertThat(result.total()).isEqualByComparingTo("6.00");
    }

    @Test
    void exposesAStableQuoteForShipmentPersistence() {
        var result = service.quote(new PricingQuoteRequest(plan.id(), "bus_pt_24h", new BigDecimal("4"), 1,
                null, null, null));

        assertThat(result.routeId()).isEqualTo(route.id());
        assertThat(result.serviceCode()).isEqualTo("BUSINESS_PARCEL");
        assertThat(result.total()).isEqualByComparingTo("6.00");
    }

    @Test
    void appliesVolumetricWeightAndAdditionalKilogramSteps() {
        var result = service.simulate(new SimulationRequest(plan.id(), "BUS_PT_24H", new BigDecimal("10"), 1,
                new BigDecimal("100"), new BigDecimal("50"), new BigDecimal("40")));
        assertThat(result.volumetricWeightKg()).isEqualByComparingTo("33.400");
        assertThat(result.additionalSteps()).isEqualTo(4);
        assertThat(result.basePrice()).isEqualByComparingTo("10.96");
        assertThat(result.total()).isEqualByComparingTo("14.43");
    }

    @Test
    void calculatesExpressParcelWithAdditionalKilograms() {
        var express = PricingRoute.create(plan, PricingRoute.ServiceCode.EXPRESS_PARCEL, "EXP_ES_1900",
                "Espanha 19h", "ES", "19h", new BigDecimal("167"), new BigDecimal("40"),
                new BigDecimal("300"), BigDecimal.ONE, new BigDecimal("0.65"), true, 2);
        express.replaceBrackets(List.of(
                new BracketRequest(BigDecimal.ONE, new BigDecimal("6.44")),
                new BracketRequest(new BigDecimal("15"), new BigDecimal("10.17")),
                new BracketRequest(new BigDecimal("30"), new BigDecimal("14.89"))));
        plan.addRoute(express, "tester");
        when(routes.findByPlanIdAndCodeIgnoreCase(plan.id(), "EXP_ES_1900")).thenReturn(Optional.of(express));

        var result = service.simulate(new SimulationRequest(plan.id(), "EXP_ES_1900", new BigDecimal("32.1"), 1,
                null, null, null));

        assertThat(result.additionalSteps()).isEqualTo(3);
        assertThat(result.basePrice()).isEqualByComparingTo("16.84");
        assertThat(result.fuelSurcharge()).isEqualByComparingTo("1.18");
        assertThat(result.vat()).isEqualByComparingTo("4.14");
        assertThat(result.total()).isEqualByComparingTo("22.16");
    }

    @Test
    void rejectsIncompleteDimensions() {
        assertThatThrownBy(() -> service.simulate(new SimulationRequest(plan.id(), "BUS_PT_24H",
                new BigDecimal("4"), 1, new BigDecimal("10"), null, new BigDecimal("20"))))
                .isInstanceOf(PricingException.class)
                .extracting(error -> ((PricingException) error).reason())
                .isEqualTo(PricingException.Reason.INVALID_CONFIGURATION);
    }

    @Test
    void rejectsWeightAbovePerParcelLimit() {
        assertThatThrownBy(() -> service.simulate(new SimulationRequest(plan.id(), "BUS_PT_24H",
                new BigDecimal("41"), 1, null, null, null)))
                .isInstanceOf(PricingException.class)
                .extracting(error -> ((PricingException) error).reason())
                .isEqualTo(PricingException.Reason.OUT_OF_RANGE);
    }

    @Test
    void requiresBothSupportedServicesBeforeActivation() {
        assertThatThrownBy(() -> service.setStatus(plan.id(), new StatusRequest(PricingPlan.Status.ACTIVE), "admin"))
                .isInstanceOf(PricingException.class)
                .extracting(error -> ((PricingException) error).reason())
                .isEqualTo(PricingException.Reason.INVALID_CONFIGURATION);
    }

    @Test
    void activationLocksThePlanAndItsRoutesForEditing() {
        var express = PricingRoute.create(plan, PricingRoute.ServiceCode.EXPRESS_PARCEL, "EXP_PT_14H",
                "Portugal 14h", "PT", "14h", new BigDecimal("167"), new BigDecimal("40"),
                new BigDecimal("300"), BigDecimal.ONE, new BigDecimal("0.32"), true, 2);
        var expressBrackets = List.of(new BracketRequest(new BigDecimal("30"), new BigDecimal("13.25")));
        express.replaceBrackets(expressBrackets);
        plan.addRoute(express, "tester");

        var activated = service.setStatus(plan.id(), new StatusRequest(PricingPlan.Status.ACTIVE), "admin");

        assertThat(activated.status()).isEqualTo(PricingPlan.Status.ACTIVE);
        assertThatThrownBy(() -> express.update("Alterado", "PT", "14h", new BigDecimal("167"),
                new BigDecimal("40"), new BigDecimal("300"), BigDecimal.ONE, new BigDecimal("0.32"),
                true, 2, expressBrackets))
                .isInstanceOf(PricingException.class)
                .extracting(error -> ((PricingException) error).reason())
                .isEqualTo(PricingException.Reason.INVALID_STATE);
    }
}
