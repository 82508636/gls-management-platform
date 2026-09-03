package pt.glsmanagement.security;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RemoteIdentityAccessTokenValidatorTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void installsTheIdentityAwareDecoderBeforeTheSpringDefault() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        OAuth2ResourceServerAutoConfiguration.class,
                        IdentityAwareJwtDecoderAutoConfiguration.class))
                .withPropertyValues(
                        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://issuer.test/realms/ltft",
                        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=http://issuer.test/certs",
                        "ltft.security.identity.base-url=http://identity.test")
                .run(context -> {
                    assertThat(context).hasSingleBean(JwtDecoder.class);
                    assertThat(context).hasBean("identityAwareJwtDecoder");
                });
    }

    @Test
    void acceptsAnExplicitPositiveDecisionAndForwardsTheSameToken() throws Exception {
        server = server((exchange) -> {
            assertThat(exchange.getRequestHeaders().getFirst("Authorization")).isEqualTo("Bearer issued-token");
            respond(exchange, 200, "{\"allowed\":true}");
        });

        var result = validator(server).validate(jwt());

        assertThat(result.hasErrors()).isFalse();
    }

    @Test
    void rejectsAnExplicitRevocation() throws Exception {
        server = server((exchange) -> respond(exchange, 200, "{\"allowed\":false}"));

        var result = validator(server).validate(jwt());

        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors().iterator().next().getErrorCode()).isEqualTo("invalid_token");
    }

    @Test
    void failsClosedWhenIdentityIsUnavailable() {
        var validator = new RemoteIdentityAccessTokenValidator(
                RestClient.builder().baseUrl("http://127.0.0.1:1").build());

        assertThat(validator.validate(jwt()).hasErrors()).isTrue();
    }

    private static RemoteIdentityAccessTokenValidator validator(HttpServer server) {
        return new RemoteIdentityAccessTokenValidator(RestClient.builder()
                .baseUrl("http://127.0.0.1:" + server.getAddress().getPort()).build());
    }

    private static HttpServer server(com.sun.net.httpserver.HttpHandler handler) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/v1/access-decisions/self", handler);
        server.start();
        return server;
    }

    private static void respond(com.sun.net.httpserver.HttpExchange exchange, int status, String json)
            throws java.io.IOException {
        var body = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (var output = exchange.getResponseBody()) { output.write(body); }
    }

    private static Jwt jwt() {
        var now = Instant.now();
        return Jwt.withTokenValue("issued-token").header("alg", "none").subject("subject")
                .issuedAt(now).expiresAt(now.plusSeconds(300)).build();
    }
}
