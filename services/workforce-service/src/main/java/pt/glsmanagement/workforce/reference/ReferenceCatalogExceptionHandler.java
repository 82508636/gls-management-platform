package pt.glsmanagement.workforce.reference;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@RestControllerAdvice(assignableTypes = ReferenceCatalogController.class)
class ReferenceCatalogExceptionHandler {
    @ExceptionHandler(ReferenceCatalogException.class)
    ResponseEntity<ApiError> handle(ReferenceCatalogException exception) {
        var status = exception.reason() == ReferenceCatalogException.Reason.NOT_FOUND
                ? HttpStatus.NOT_FOUND : HttpStatus.CONFLICT;
        var message = status == HttpStatus.NOT_FOUND
                ? "Não foi possível encontrar o recurso solicitado." : "Não foi possível guardar os dados.";
        return ResponseEntity.status(status).body(error(status, message));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict(DataIntegrityViolationException ignored) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error(HttpStatus.CONFLICT, "Não foi possível guardar os dados."));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class})
    ResponseEntity<ApiError> validation(Exception ignored) {
        return ResponseEntity.badRequest()
                .body(error(HttpStatus.BAD_REQUEST, "Não foi possível validar os dados enviados."));
    }

    private static ApiError error(HttpStatus status, String message) {
        return new ApiError(OffsetDateTime.now(ZoneOffset.UTC), status.value(), message, Map.of());
    }

    record ApiError(OffsetDateTime timestamp, int status, String message, Map<String, String> fields) {}
}
