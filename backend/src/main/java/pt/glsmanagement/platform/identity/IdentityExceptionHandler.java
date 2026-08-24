package pt.glsmanagement.platform.identity;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientResponseException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestControllerAdvice(assignableTypes = IdentityAdminController.class)
class IdentityExceptionHandler {
    @ExceptionHandler({IdentityOperationException.class, RestClientResponseException.class})
    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    ErrorResponse providerFailure() { return error(HttpStatus.BAD_GATEWAY, "Não foi possível concluir a operação de utilizador."); }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ErrorResponse invalidOperation() { return error(HttpStatus.BAD_REQUEST, "Não foi possível concluir a operação solicitada."); }

    private ErrorResponse error(HttpStatus status, String message) {
        return new ErrorResponse(OffsetDateTime.now(ZoneOffset.UTC), status.value(), message);
    }
    record ErrorResponse(OffsetDateTime timestamp, int status, String message) {}
}
