package pt.glsmanagement.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers.mockJwt;

@WebFluxTest(controllers = GatewaySecurityTest.TestController.class)
@Import({SecurityConfig.class, CorrelationIdFilter.class, GatewayErrorResponseWriter.class})
class GatewaySecurityTest {
    @MockitoBean ReactiveJwtDecoder jwtDecoder;

    @Autowired WebTestClient client;

    @Test
    void rejectsAnonymousApiRequestsWithTheSharedErrorContract() {
        client.get().uri("/api/test").exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().contentTypeCompatibleWith(MediaType.APPLICATION_JSON)
                .expectHeader().exists(CorrelationIdFilter.HEADER)
                .expectHeader().valueEquals(HttpHeaders.CACHE_CONTROL, "no-store")
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.traceId").isNotEmpty()
                .jsonPath("$.message").isEqualTo("É necessário iniciar sessão para continuar.");
    }

    @Test
    void letsAnAuthenticatedTokenReachRoutingWithoutPublishingUnknownApiPaths() {
        client.mutateWith(mockJwt()).get().uri("/api/test").exchange()
                .expectStatus().isNotFound()
                .expectHeader().exists(CorrelationIdFilter.HEADER)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo("O recurso pedido não existe.");
    }

    @Test
    void deniesAuthenticatedRequestsOutsideThePublishedSurface() {
        client.mutateWith(mockJwt()).get().uri("/private/test").exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403);
    }

    @RestController
    static class TestController {
        @GetMapping("/private/test") String privateRoute() { return "hidden"; }
    }
}
