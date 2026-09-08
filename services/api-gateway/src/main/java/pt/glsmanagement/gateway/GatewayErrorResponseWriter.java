package pt.glsmanagement.gateway;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;

@Component
final class GatewayErrorResponseWriter {
    private final ObjectMapper objectMapper;

    GatewayErrorResponseWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    Mono<Void> write(ServerHttpResponse response, HttpStatusCode status, String message) {
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        response.getHeaders().set(HttpHeaders.CACHE_CONTROL, "no-store");
        var body = new LinkedHashMap<String, Object>();
        body.put("timestamp", OffsetDateTime.now(ZoneOffset.UTC));
        body.put("status", status.value());
        body.put("message", message);
        String traceId = response.getHeaders().getFirst(CorrelationIdFilter.HEADER);
        if (traceId != null) {
            body.put("traceId", traceId);
        }
        try {
            byte[] bytes = objectMapper.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(bytes)));
        } catch (Exception serializationFailure) {
            return response.setComplete();
        }
    }
}
