package pt.glsmanagement.platform.customer;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@RestControllerAdvice(assignableTypes = CustomerController.class)
class CustomerExceptionHandler {
    @ExceptionHandler(CustomerNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    ApiError notFound(CustomerNotFoundException ignored) {
        return error(HttpStatus.NOT_FOUND, "Não foi possível encontrar o recurso solicitado.");
    }

    @ExceptionHandler({DuplicateCustomerVatNumberException.class, DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    ApiError duplicate(RuntimeException ignored) {
        return error(HttpStatus.CONFLICT, "Não foi possível guardar os dados.");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError validation(MethodArgumentNotValidException ignored) {
        return error(HttpStatus.BAD_REQUEST, "Não foi possível validar os dados enviados.");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    ApiError invalidOperation(IllegalArgumentException ignored) {
        return error(HttpStatus.BAD_REQUEST, "Não foi possível concluir a operação solicitada.");
    }

    private static ApiError error(HttpStatus status, String message) {
        return new ApiError(OffsetDateTime.now(ZoneOffset.UTC), status.value(), message, Map.of());
    }

    record ApiError(OffsetDateTime timestamp, int status, String message, Map<String, String> fields) {}
}
