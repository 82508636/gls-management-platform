package pt.glsmanagement.platform.pricing;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import pt.glsmanagement.platform.web.ApiError;

@RestControllerAdvice(assignableTypes = PricingController.class)
class PricingExceptionHandler {
    @ExceptionHandler(PricingException.class)
    ResponseEntity<ApiError> handle(PricingException exception) {
        var status = switch (exception.reason()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case DUPLICATE, INVALID_STATE -> HttpStatus.CONFLICT;
            case INVALID_CONFIGURATION, OUT_OF_RANGE -> HttpStatus.BAD_REQUEST;
        };
        var message = status == HttpStatus.NOT_FOUND ? "Não foi possível encontrar o recurso solicitado."
                : status == HttpStatus.CONFLICT ? "Não foi possível guardar os dados."
                : "Não foi possível calcular com os dados enviados.";
        return ResponseEntity.status(status).body(ApiError.of(status.value(), message));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException exception) {
        return ResponseEntity.badRequest().body(ApiError.of(400, "Não foi possível validar os dados enviados."));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict(DataIntegrityViolationException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError.of(409, "Não foi possível guardar os dados."));
    }
}
