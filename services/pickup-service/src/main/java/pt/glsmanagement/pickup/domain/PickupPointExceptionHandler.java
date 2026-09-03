package pt.glsmanagement.pickup.domain;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@RestControllerAdvice(assignableTypes = PickupPointController.class)
class PickupPointExceptionHandler {
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
            IllegalArgumentException.class})
    ResponseEntity<ApiError> invalid(Exception ignored) {
        return response(HttpStatus.BAD_REQUEST, "Não foi possível validar os dados enviados.");
    }

    @ExceptionHandler(PickupPointNotFoundException.class)
    ResponseEntity<ApiError> notFound(PickupPointNotFoundException ignored) {
        return response(HttpStatus.NOT_FOUND, "Não foi possível encontrar o ponto Pickup.");
    }

    @ExceptionHandler(DuplicatePickupPointException.class)
    ResponseEntity<ApiError> conflict(DuplicatePickupPointException ignored) {
        return response(HttpStatus.CONFLICT, "Já existe um ponto Pickup com o mesmo código.");
    }

    private static ResponseEntity<ApiError> response(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ApiError(OffsetDateTime.now(ZoneOffset.UTC), status.value(), message));
    }

    record ApiError(OffsetDateTime timestamp, int status, String message) {}
}
