package pt.glsmanagement.platform.shipment;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import pt.glsmanagement.platform.pricing.PricingQuoteRequest;

import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemotePricingQuoteServiceTest {
    private HttpServer server;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        if (server != null) server.stop(0);
    }

    @Test
    void forwardsTheCurrentJwtAndReturnsTheRemoteQuote() throws Exception {
        var planId = UUID.randomUUID();
        var routeId = UUID.randomUUID();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/v1/pricing/quotes", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("Authorization"))
                    .isEqualTo("Bearer shipment-token");
            var request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(request).contains(planId.toString(), "ROTA_PT", "actualWeightKg");
            var body = ("""
                    {"planId":"%s","planCode":"LTFT-BASE","planVersion":1,
                    "routeId":"%s","routeCode":"ROTA_PT","routeDesignation":"Portugal",
                    "destinationCountry":"PT","actualWeightKg":4,"volumetricWeightKg":0,
                    "chargeableWeightKg":4,"basePrice":5,"fuelSurcharge":0.5,
                    "subtotal":5.5,"vat":1.27,"total":6.77,"currency":"EUR"}
                    """).formatted(planId, routeId).getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt()));
        var service = service(Duration.ofSeconds(1));

        var result = service.quote(request(planId));

        assertThat(result.routeId()).isEqualTo(routeId);
        assertThat(result.total()).isEqualByComparingTo("6.77");
    }

    @Test
    void failsClosedWithoutAUserJwt() {
        var service = service(Duration.ofMillis(100));

        assertReason(() -> service.quote(request(UUID.randomUUID())),
                ShipmentException.Reason.PRICING_UNAVAILABLE);
    }

    @Test
    void mapsRemoteValidationFailuresToInvalidShipmentData() throws Exception {
        startErrorServer(400);
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt()));

        assertReason(() -> service(Duration.ofSeconds(1)).quote(request(UUID.randomUUID())),
                ShipmentException.Reason.INVALID_DATA);
    }

    @Test
    void mapsRemoteOutageToServiceUnavailable() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt()));

        assertReason(() -> service(Duration.ofMillis(100)).quote(request(UUID.randomUUID())),
                ShipmentException.Reason.PRICING_UNAVAILABLE);
    }

    private RemotePricingQuoteService service(Duration timeout) {
        var url = server == null ? "http://127.0.0.1:1" : "http://127.0.0.1:" + server.getAddress().getPort();
        return new RemotePricingQuoteService(url, timeout, timeout);
    }

    private void startErrorServer(int status) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/v1/pricing/quotes", exchange -> {
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
    }

    private static PricingQuoteRequest request(UUID planId) {
        return new PricingQuoteRequest(planId, "ROTA_PT", new BigDecimal("4"), 1,
                null, null, null);
    }

    private static Jwt jwt() {
        var now = Instant.now();
        return Jwt.withTokenValue("shipment-token").header("alg", "none").subject("operator")
                .issuedAt(now).expiresAt(now.plusSeconds(300)).build();
    }

    private static void assertReason(org.assertj.core.api.ThrowableAssert.ThrowingCallable operation,
                                     ShipmentException.Reason expected) {
        assertThatThrownBy(operation).isInstanceOf(ShipmentException.class)
                .extracting(error -> ((ShipmentException) error).reason()).isEqualTo(expected);
    }
}
