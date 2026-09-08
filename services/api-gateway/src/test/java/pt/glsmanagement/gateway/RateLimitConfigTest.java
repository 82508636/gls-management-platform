package pt.glsmanagement.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.Principal;

class RateLimitConfigTest {
    private final RateLimitConfig configuration = new RateLimitConfig();

    @Test
    void usesTheAuthenticatedPrincipalAsTheDistributedLimitKey() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/customers"))
                .mutate()
                .principal(Mono.just((Principal) () -> "user-123"))
                .build();

        StepVerifier.create(configuration.authenticatedPrincipalKeyResolver().resolve(exchange))
                .expectNext("user-123")
                .verifyComplete();
    }

    @Test
    void doesNotCreateAnAnonymousSharedBucket() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/customers"));

        StepVerifier.create(configuration.authenticatedPrincipalKeyResolver().resolve(exchange))
                .verifyComplete();
    }
}
