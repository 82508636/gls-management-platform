package pt.glsmanagement.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.function.Function;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class GatewayRoutesTest {
    @MockitoBean ReactiveJwtDecoder jwtDecoder;

    @Autowired RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void publishesOnlyTheExpectedRouteDefinitions() {
        var routes = routeDefinitionLocator.getRouteDefinitions().collectList().block();

        assertThat(routes).isNotNull();
        assertThat(routes).extracting(RouteDefinition::getId).containsExactlyInAnyOrder(
                "legacy-customer-services",
                "legacy-shipments",
                "customer-service",
                "pickup-service",
                "workforce-service",
                "identity-service",
                "catalog-service",
                "pricing-service");
    }

    @Test
    void prioritizesLegacyCustomerServicesBeforeTheGeneralCustomerRoute() {
        var routesById = routeDefinitionLocator.getRouteDefinitions().collectList().block().stream()
                .collect(Collectors.toMap(RouteDefinition::getId, Function.identity()));

        assertThat(routesById.get("legacy-customer-services").getOrder())
                .isLessThan(routesById.get("customer-service").getOrder());
    }
}
