package pt.glsmanagement.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;
import reactor.test.StepVerifier;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayExceptionHandlerTest {
    private final GatewayExceptionHandler handler = new GatewayExceptionHandler(
            new GatewayErrorResponseWriter(new ObjectMapper().findAndRegisterModules()));

    @Test
    void hidesConnectionDetailsBehindAServiceUnavailableResponse() {
        var exchange = exchangeWithTraceId();

        StepVerifier.create(handler.handle(exchange, new IllegalStateException(
                "internal host", new ConnectException("connection refused to customer-postgres"))))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("O serviço está temporariamente indisponível.")
                .contains("request-123")
                .doesNotContain("customer-postgres");
    }

    @Test
    void reportsGatewayTimeoutWithoutLeakingTheFailure() {
        var exchange = exchangeWithTraceId();

        StepVerifier.create(handler.handle(exchange, new TimeoutException("secret downstream details")))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.GATEWAY_TIMEOUT);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("O serviço demorou demasiado tempo a responder.")
                .doesNotContain("secret downstream details");
    }

    @Test
    void preservesAProtocolStatusButHidesItsInternalReason() {
        var exchange = exchangeWithTraceId();

        StepVerifier.create(handler.handle(exchange,
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "internal route table details")))
                .verifyComplete();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(exchange.getResponse().getBodyAsString().block())
                .contains("O recurso pedido não existe.")
                .doesNotContain("internal route table details");
    }

    private static MockServerWebExchange exchangeWithTraceId() {
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/customers"));
        exchange.getResponse().getHeaders().set(CorrelationIdFilter.HEADER, "request-123");
        return exchange;
    }
}
