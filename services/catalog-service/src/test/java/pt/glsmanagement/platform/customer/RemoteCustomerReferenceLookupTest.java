package pt.glsmanagement.platform.customer;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RemoteCustomerReferenceLookupTest {
    private HttpServer server;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        if (server != null) server.stop(0);
    }

    @Test
    void emptyReferencesNeedNoRemoteCallOrAuthentication() {
        var lookup = new RemoteCustomerReferenceLookup(
                "http://127.0.0.1:1", Duration.ofMillis(100), Duration.ofMillis(100));

        assertThat(lookup.allExist(Set.of())).isTrue();
    }

    @Test
    void refusesToValidateWithoutAUserToken() {
        var lookup = new RemoteCustomerReferenceLookup(
                "http://127.0.0.1:1", Duration.ofMillis(100), Duration.ofMillis(100));

        assertThatThrownBy(() -> lookup.allExist(Set.of(UUID.randomUUID())))
                .isInstanceOf(CustomerReferenceUnavailableException.class);
    }

    @Test
    void forwardsTheJwtAndUsesTheCustomerDecision() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/v1/customers/existence", exchange -> {
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer catalog-token");
            var request = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            assertThat(request).contains("ids");
            var body = "{\"allExist\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var output = exchange.getResponseBody()) { output.write(body); }
        });
        server.start();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt()));
        var lookup = new RemoteCustomerReferenceLookup(
                "http://127.0.0.1:" + server.getAddress().getPort(), Duration.ofSeconds(1), Duration.ofSeconds(1));

        assertThat(lookup.allExist(Set.of(UUID.randomUUID()))).isTrue();
    }

    @Test
    void failsClosedWhenCustomerServiceIsUnavailable() {
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt()));
        var lookup = new RemoteCustomerReferenceLookup(
                "http://127.0.0.1:1", Duration.ofMillis(100), Duration.ofMillis(100));

        assertThatThrownBy(() -> lookup.allExist(Set.of(UUID.randomUUID())))
                .isInstanceOf(CustomerReferenceUnavailableException.class);
    }

    private static Jwt jwt() {
        var now = Instant.now();
        return Jwt.withTokenValue("catalog-token").header("alg", "none").subject("admin")
                .issuedAt(now).expiresAt(now.plusSeconds(300)).build();
    }
}
