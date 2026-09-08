package pt.glsmanagement.gateway;

import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;
import java.util.regex.Pattern;

@Component
final class CorrelationIdFilter implements WebFilter, Ordered {
    static final String HEADER = "X-Correlation-ID";
    private static final Pattern SAFE_VALUE = Pattern.compile("[A-Za-z0-9._:-]{1,100}");

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String supplied = exchange.getRequest().getHeaders().getFirst(HEADER);
        String correlationId = supplied != null && SAFE_VALUE.matcher(supplied).matches()
                ? supplied
                : UUID.randomUUID().toString();
        var request = exchange.getRequest().mutate().headers(headers -> {
            headers.remove(HEADER);
            headers.add(HEADER, correlationId);
        }).build();
        var response = exchange.getResponse();
        response.getHeaders().set(HEADER, correlationId);
        response.getHeaders().set(HttpHeaders.CACHE_CONTROL, "no-store");
        response.beforeCommit(() -> {
            response.getHeaders().set(HEADER, correlationId);
            response.getHeaders().set(HttpHeaders.CACHE_CONTROL, "no-store");
            return Mono.empty();
        });
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
