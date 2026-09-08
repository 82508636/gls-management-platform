package pt.glsmanagement.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class CorrelationIdFilterTest {
    private final CorrelationIdFilter filter = new CorrelationIdFilter();

    @Test
    void preservesSafeCorrelationIdAndPropagatesItBothWays() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/customers")
                .header(CorrelationIdFilter.HEADER, "request-123"));
        var forwarded = new AtomicReference<String>();

        StepVerifier.create(filter.filter(exchange, current -> {
            forwarded.set(current.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER));
            return Mono.empty();
        })).verifyComplete();

        assertThat(forwarded).hasValue("request-123");
        assertThat(exchange.getResponse().getHeaders().getFirst(CorrelationIdFilter.HEADER))
                .isEqualTo("request-123");
        assertThat(exchange.getResponse().getHeaders().getCacheControl()).isEqualTo("no-store");
    }

    @Test
    void replacesUnsafeCorrelationId() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/customers")
                .header(CorrelationIdFilter.HEADER, "invalid value with spaces"));
        var forwarded = new AtomicReference<String>();

        StepVerifier.create(filter.filter(exchange, current -> {
            forwarded.set(current.getRequest().getHeaders().getFirst(CorrelationIdFilter.HEADER));
            return Mono.empty();
        })).verifyComplete();

        assertThat(forwarded.get()).isNotBlank().doesNotContain(" ");
        assertThat(forwarded.get()).isEqualTo(exchange.getResponse().getHeaders()
                .getFirst(CorrelationIdFilter.HEADER));
    }
}
