package pt.glsmanagement.platform.entity;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@RestControllerAdvice(assignableTypes = {PickupPointController.class, RecipientController.class})
class EntityExceptionHandler {
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class, IllegalArgumentException.class})
    ResponseEntity<Map<String, Object>> invalid(Exception ignored) { return response(HttpStatus.BAD_REQUEST, "Não foi possível validar os dados enviados."); }

    @ExceptionHandler(EntityNotFoundException.class)
    ResponseEntity<Map<String, Object>> notFound(EntityNotFoundException ignored) { return response(HttpStatus.NOT_FOUND, "Não foi possível encontrar o registo."); }

    @ExceptionHandler(EntityOperationException.class)
    ResponseEntity<Map<String, Object>> conflict(EntityOperationException ignored) { return response(HttpStatus.CONFLICT, "Não foi possível guardar o registo."); }

    private static ResponseEntity<Map<String, Object>> response(HttpStatus status, String message) {
        return ResponseEntity.status(status).body(Map.of("timestamp", OffsetDateTime.now(ZoneOffset.UTC), "status", status.value(), "message", message));
    }
}
