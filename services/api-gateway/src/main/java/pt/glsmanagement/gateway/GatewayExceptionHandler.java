package pt.glsmanagement.gateway;

import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebExceptionHandler;
import reactor.core.publisher.Mono;

import java.net.ConnectException;
import java.util.concurrent.TimeoutException;

@Component
@Order(-2)
final class GatewayExceptionHandler implements WebExceptionHandler {
    private final GatewayErrorResponseWriter responseWriter;

    GatewayExceptionHandler(GatewayErrorResponseWriter responseWriter) {
        this.responseWriter = responseWriter;
    }

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable failure) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(failure);
        }
        HttpStatusCode status = statusFor(failure);
        return responseWriter.write(exchange.getResponse(), status, messageFor(status));
    }

    private static HttpStatusCode statusFor(Throwable failure) {
        if (failure instanceof ResponseStatusException responseStatusException) {
            return responseStatusException.getStatusCode();
        }
        if (hasCause(failure, ConnectException.class)) {
            return HttpStatus.SERVICE_UNAVAILABLE;
        }
        if (hasCause(failure, TimeoutException.class) || hasTimeoutCause(failure)) {
            return HttpStatus.GATEWAY_TIMEOUT;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static String messageFor(HttpStatusCode status) {
        return switch (status.value()) {
            case 400 -> "O pedido é inválido.";
            case 401 -> "É necessário iniciar sessão para continuar.";
            case 403 -> "Não tem permissão para realizar esta operação.";
            case 404 -> "O recurso pedido não existe.";
            case 405 -> "O método HTTP não é permitido para este recurso.";
            case 409 -> "O pedido entra em conflito com o estado atual do recurso.";
            case 413 -> "O pedido excede o tamanho permitido.";
            case 429 -> "Foram efetuados demasiados pedidos. Tente novamente dentro de momentos.";
            case 503 -> "O serviço está temporariamente indisponível.";
            case 504 -> "O serviço demorou demasiado tempo a responder.";
            default -> "Ocorreu um erro interno ao processar o pedido.";
        };
    }

    private static boolean hasCause(Throwable failure, Class<? extends Throwable> type) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (type.isInstance(current)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasTimeoutCause(Throwable failure) {
        for (Throwable current = failure; current != null; current = current.getCause()) {
            if (current.getClass().getSimpleName().contains("TimeoutException")) {
                return true;
            }
        }
        return false;
    }
}
